package com.zzowo.shop_sys.service;

import com.zzowo.shop_sys.dto.request.order.OrderCreateRequest;
import com.zzowo.shop_sys.dto.response.order.OrderResponse;
import com.zzowo.shop_sys.entity.*;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.mapper.OrderMapper;
import com.zzowo.shop_sys.repository.CartRepository;
import com.zzowo.shop_sys.repository.InventoryLogRepository;
import com.zzowo.shop_sys.repository.OrderRepository;
import com.zzowo.shop_sys.repository.ProductRepository;
import com.zzowo.shop_sys.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private InventoryLogRepository inventoryLogRepository;

    // 建立訂單 (結帳)
    @Transactional // 確保庫存扣減、訂單建立、清空購物車同時成功或失敗
    public OrderResponse createOrder(String email, OrderCreateRequest request) {
        // 確認使用者身分
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("使用者不存在"));

        // 取得購物車
        List<Cart> cartItems = cartRepository.findByUserId(user.getId());
        if (cartItems.isEmpty()) {
            throw new RuntimeException("購物車為空，無法結帳");
        }

        // 準備建立訂單
        Order order = new Order();
        order.setUser(user);
        order.setStatus("PENDING"); // 初始狀態：待付款
        order.setRecipientName(request.getRecipientName());
        order.setRecipientPhone(request.getRecipientPhone());
        order.setRecipientAddress(request.getRecipientAddress());

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 處理每個購物車商品 (檢查庫存、扣庫存、建立 OrderItem)
        for (Cart cart : cartItems) {
            Product product = cart.getProduct();

            // 檢查庫存 (使用悲觀鎖或樂觀鎖會更嚴謹，這裡先做基礎檢查)
            if (product.getStockQuantity() < cart.getQuantity()) {
                throw new RuntimeException("商品 [" + product.getName() + "] 庫存不足，結帳失敗");
            }

            // 扣除庫存
            Integer quantityToDeduct = cart.getQuantity(); // 購買數量
            product.setStockQuantity(product.getStockQuantity() - quantityToDeduct);
            productRepository.save(product);

            // 建立庫存異動紀錄
            InventoryLog log = new InventoryLog();
            log.setProduct(product);
            log.setChangeAmount(-quantityToDeduct); // 負數代表減少
            log.setReason("ORDER"); // 原因：訂單出貨
            log.setOperatorId(user.getId()); // 操作者：買家 (或可設為 null，視你的定義)
            inventoryLogRepository.save(log);

            // 建立訂單明細
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setPriceAtPurchase(product.getPrice()); // 鎖定當下價格
            item.setQuantity(cart.getQuantity());

            orderItems.add(item);

            // 累加總金額
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
            totalAmount = totalAmount.add(subtotal);
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);

        // 儲存訂單 (Cascade 會自動儲存 OrderItems)
        Order savedOrder = orderRepository.save(order);

        // 清空購物車
        cartRepository.deleteByUserId(user.getId());

        return orderMapper.toOrderResponse(savedOrder);
    }

    // 查看我的訂單
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("使用者不存在"));

        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return orders.stream().map(orderMapper::toOrderResponse).collect(Collectors.toList());
    }

    // 查看單一訂單詳情
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String email, Long orderId) {
        // 確認使用者身分
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("使用者不存在"));

        // 取得訂單
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("訂單不存在"));

        // 權限檢查：只能查看自己的訂單
        if (!order.getUser().getId().equals(user.getId()) && user.getRole().name().equals("CUSTOMER")) {
             throw new RuntimeException("無權限查看此訂單");
        }

        return orderMapper.toOrderResponse(order);
    }
}