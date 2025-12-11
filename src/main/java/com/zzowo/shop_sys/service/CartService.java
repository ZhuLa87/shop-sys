package com.zzowo.shop_sys.service;

import com.zzowo.shop_sys.dto.request.cart.AddToCartRequest;
import com.zzowo.shop_sys.dto.response.cart.CartItemResponse;
import com.zzowo.shop_sys.entity.Cart;
import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.mapper.CartMapper;
import com.zzowo.shop_sys.repository.CartRepository;
import com.zzowo.shop_sys.repository.ProductRepository;
import com.zzowo.shop_sys.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartMapper cartMapper;

    // 取得某使用者的購物車清單
    public List<CartItemResponse> getUserCart(String email) {
        User user = getUserByEmail(email);
        List<Cart> carts = cartRepository.findByUserId(user.getId());

        return carts.stream()
                .map(cartMapper::toCartItemResponse)
                .collect(Collectors.toList());
    }

    // 加入購物車
    @Transactional
    public void addToCart(String email, AddToCartRequest request) {
        User user = getUserByEmail(email);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在"));

        // 檢查庫存
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new RuntimeException("庫存不足");
        }

        // 檢查購物車是否已經有該商品，若有則增加數量，若無則新增
        Cart cart = cartRepository.findByUserIdAndProductId(user.getId(), product.getId())
                .orElse(new Cart());

        if (cart.getId() == null) {
            // 新增
            cart.setUser(user);
            cart.setProduct(product);
            cart.setQuantity(request.getQuantity());
        } else {
            // 既有商品，累加數量
            cart.setQuantity(cart.getQuantity() + request.getQuantity());
        }

        cartRepository.save(cart);
    }

    // 移除購物車項目
    @Transactional
    public void removeFromCart(String email, Long cartId) {
        User user = getUserByEmail(email);
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("購物車資料不存在"));

        // 確保只能刪除自己的購物車
        if (!cart.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("無權限操作此購物車");
        }

        cartRepository.delete(cart);
    }

    // 輔助方法：用 Email 找 User
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("使用者不存在"));
    }
}