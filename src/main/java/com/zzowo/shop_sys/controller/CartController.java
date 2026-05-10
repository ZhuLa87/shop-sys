package com.zzowo.shop_sys.controller;

import com.zzowo.shop_sys.dto.request.cart.AddToCartRequest;
import com.zzowo.shop_sys.dto.response.ApiResponse;
import com.zzowo.shop_sys.dto.response.cart.CartItemResponse;
import com.zzowo.shop_sys.service.CartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/carts")
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 查看我的購物車
     * @param userDetails
     * @return
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CartItemResponse>>> getMyCart(@AuthenticationPrincipal String email) {
        List<CartItemResponse> cartItems = cartService.getUserCart(email);
        return ResponseEntity.ok(ApiResponse.success("取得購物車成功", cartItems));
    }

    /**
     * 加入購物車
     * @param userDetails
     * @param request
     * @return
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addToCart(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody AddToCartRequest request) {
        cartService.addToCart(email, request);
        return ResponseEntity.ok(ApiResponse.success("加入購物車成功"));
    }

    /**
     * 刪除購物車項目
     * @param userDetails
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCartItem(
            @AuthenticationPrincipal String email,
            @PathVariable Long id) {
        cartService.removeFromCart(email, id);
        return ResponseEntity.ok(ApiResponse.success("移除商品成功"));
    }
}