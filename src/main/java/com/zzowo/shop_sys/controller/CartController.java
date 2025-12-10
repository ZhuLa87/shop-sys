package com.zzowo.shop_sys.controller;

import com.zzowo.shop_sys.dto.request.cart.AddToCartRequest;
import com.zzowo.shop_sys.dto.response.ApiResponse;
import com.zzowo.shop_sys.dto.response.cart.CartItemResponse;
import com.zzowo.shop_sys.service.CartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    public ResponseEntity<ApiResponse<List<CartItemResponse>>> getMyCart(@AuthenticationPrincipal UserDetails userDetails) {
        // @AuthenticationPrincipal 可以直接拿到目前登入者的資訊 (由 JwtFilter 解析放入的)
        List<CartItemResponse> cartItems = cartService.getUserCart(userDetails.getUsername());
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
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddToCartRequest request) {
        cartService.addToCart(userDetails.getUsername(), request);
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
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        cartService.removeFromCart(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("移除商品成功"));
    }
}