package com.zzowo.shop_sys.service;

import lombok.RequiredArgsConstructor;
import com.zzowo.shop_sys.dto.request.cart.AddToCartRequest;
import com.zzowo.shop_sys.dto.response.cart.CartItemResponse;
import com.zzowo.shop_sys.entity.Cart;
import com.zzowo.shop_sys.entity.Product;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.ProductStatus;
import com.zzowo.shop_sys.exception.BusinessException;
import org.springframework.http.HttpStatus;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.mapper.CartMapper;
import com.zzowo.shop_sys.repository.CartRepository;
import com.zzowo.shop_sys.repository.ProductRepository;
import com.zzowo.shop_sys.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;

    private final ProductRepository productRepository;

    private final UserRepository userRepository;

    private final CartMapper cartMapper;

    @Transactional
    public List<CartItemResponse> getUserCart(String email) {
        User user = userRepository.getByEmailOrThrow(email);
        List<Cart> carts = cartRepository.findByUserId(user.getId());

        // 過濾並自動清除購物車中已被軟刪除的商品
        // (product 為 null 表示商品已刪除,@NotFound(IGNORE) 使 Hibernate 回傳 null 而非拋出例外)
        List<Long> staleIds = carts.stream()
                .filter(c -> c.getProduct() == null)
                .map(Cart::getId)
                .collect(Collectors.toList());
        if (!staleIds.isEmpty()) {
            cartRepository.deleteAllById(staleIds);
        }

        return carts.stream()
                .filter(c -> c.getProduct() != null)
                .map(cartMapper::toCartItemResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addToCart(String email, AddToCartRequest request) {
        User user = userRepository.getByEmailOrThrow(email);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("商品不存在"));

        // 只有上架中的商品可購買 (缺貨中/已下架即使仍有庫存也不行)
        if (product.getStatus() != ProductStatus.ON_SHELF) {
            throw new BusinessException("商品 [" + product.getName() + "] 目前" + product.getStatus().getDescription() + ",無法加入購物車");
        }

        Cart cart = cartRepository.findByUserIdAndProductId(user.getId(), product.getId())
                .orElse(new Cart());

        int existingQuantity = (cart.getId() == null) ? 0 : cart.getQuantity();
        // addExact: 溢位成負數會通過下方的庫存檢查,並在結帳時變成負數明細折抵訂單金額
        int totalTargetQuantity;
        try {
            totalTargetQuantity = Math.addExact(existingQuantity, request.getQuantity());
        } catch (ArithmeticException e) {
            throw new BusinessException("購物車數量超過上限");
        }
        if (totalTargetQuantity > AddToCartRequest.MAX_QUANTITY) {
            throw new BusinessException("每項商品最多只能加入 " + AddToCartRequest.MAX_QUANTITY + " 件,目前購物車內已有 " + existingQuantity + " 件");
        }

        if (product.getStockQuantity() < totalTargetQuantity) {
            throw new BusinessException("庫存不足,目前購物車內已有 " + existingQuantity + " 件,無法再加入 " + request.getQuantity() + " 件");
        }

        if (cart.getId() == null) {
            cart.setUser(user);
            cart.setProduct(product);
            cart.setQuantity(request.getQuantity());
        } else {
            cart.setQuantity(totalTargetQuantity);
        }

        cartRepository.save(cart);
    }

    @Transactional
    public void removeFromCart(String email, Long cartId) {
        User user = userRepository.getByEmailOrThrow(email);
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("購物車資料不存在"));

        // 確保只能刪除自己的購物車
        if (!cart.getUser().getId().equals(user.getId())) {
            throw new BusinessException("無權限操作此購物車", HttpStatus.FORBIDDEN);
        }

        cartRepository.delete(cart);
    }
}
