package com.zzowo.shop_sys.service;

import com.zzowo.shop_sys.dto.request.order.OrderCreateRequest;
import com.zzowo.shop_sys.dto.response.order.OrderResponse;
import com.zzowo.shop_sys.entity.*;
import com.zzowo.shop_sys.enums.OrderStatus;
import com.zzowo.shop_sys.enums.ProductStatus;
import com.zzowo.shop_sys.enums.Role;
import com.zzowo.shop_sys.exception.BusinessException;
import org.springframework.http.HttpStatus;
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
import java.util.Comparator;
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
    @Transactional
    public OrderResponse createOrder(String email, OrderCreateRequest request) {
        // 確認使用者身分
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("使用者不存在"));

        // 取得購物車
        List<Cart> cartItems = cartRepository.findByUserId(user.getId());
        if (cartItems.isEmpty()) {
            throw new BusinessException("購物車為空,無法結帳");
        }

        // 固定以 product id 遞增順序扣庫存,避免兩筆交易互等對方的行鎖造成死鎖
        // 商品已軟刪除 (product 為 null) 的排最前面,讓下方的檢查第一個命中
        cartItems = cartItems.stream()
                .sorted(Comparator.comparing(c -> c.getProduct() == null ? Long.MIN_VALUE : c.getProduct().getId()))
                .toList();

        // 準備建立訂單
        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING); // 使用 Enum 代替 String
        order.setRecipientName(request.getRecipientName());
        order.setRecipientPhone(request.getRecipientPhone());
        order.setRecipientAddress(request.getRecipientAddress());

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 處理每個購物車商品
        for (Cart cart : cartItems) {
            Product product = cart.getProduct();

            // 商品已被軟刪除 (@NotFound 使關聯回傳 null 而非拋出例外)
            if (product == null) {
                throw new BusinessException("購物車中有商品已下架或刪除,請重新確認購物車內容");
            }

            // 加入購物車後商品可能被改成缺貨中/已下架,結帳時再確認一次
            if (product.getStatus() != ProductStatus.ON_SHELF) {
                throw new BusinessException("商品 [" + product.getName() + "] 目前" + product.getStatus().getDescription() + ",結帳失敗");
            }

            // 快速失敗: 先用已載入的資料擋掉明顯不足的情況,省下一次沒必要的 DB 寫入
            if (product.getStockQuantity() < cart.getQuantity()) {
                throw new BusinessException("商品 [" + product.getName() + "] 庫存不足,結帳失敗");
            }

            // 真正的防超賣: 單句條件更新,由 DB 保證原子性
            // 注意扣庫存後 product 的 stockQuantity 與 version 在記憶體中已過期,不可再對它做任何修改
            Integer quantityToDeduct = cart.getQuantity();
            if (productRepository.deductStock(product.getId(), quantityToDeduct) == 0) {
                // 預檢查到此刻之間被其他交易買走 (併發搶輸),整筆交易回滾
                throw new BusinessException("商品 [" + product.getName() + "] 庫存不足,結帳失敗");
            }

            // 建立庫存異動紀錄
            InventoryLog log = new InventoryLog();
            log.setProduct(product);
            log.setChangeAmount(-quantityToDeduct);
            log.setReason("ORDER");
            log.setOperatorId(user.getId());
            inventoryLogRepository.save(log);

            // 建立訂單明細 (name/coverImageUrl 為快照,確保商品日後改名或刪除仍可正確顯示)
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setCoverImageUrl(product.getCoverImageUrl());
            item.setPriceAtPurchase(product.getPrice());
            item.setQuantity(cart.getQuantity());

            orderItems.add(item);

            // 累加總金額
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
            totalAmount = totalAmount.add(subtotal);
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);

        // 儲存訂單
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

        if (!order.getUser().getId().equals(user.getId()) && user.getRole() != Role.SUPER_ADMIN) {
             throw new BusinessException("無權限查看此訂單", HttpStatus.FORBIDDEN);
        }

        return orderMapper.toOrderResponse(order);
    }
}
