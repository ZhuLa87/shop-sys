package com.zzowo.shop_sys.controller;

import com.zzowo.shop_sys.dto.request.cart.AddToCartRequest;
import com.zzowo.shop_sys.dto.response.ApiResponse;
import com.zzowo.shop_sys.dto.response.cart.CartItemResponse;
import com.zzowo.shop_sys.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Cart", description = "購物車 API (需登入,僅能操作自己的購物車) ")
@RestController
@RequestMapping("/v1/carts")
public class CartController {

    @Autowired
    private CartService cartService;

    @Operation(summary = "查看我的購物車", description = "取得目前登入使用者的購物車所有項目")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取得成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CartItemResponse>>> getMyCart(
            @Parameter(hidden = true) @AuthenticationPrincipal String email) {
        List<CartItemResponse> cartItems = cartService.getUserCart(email);
        return ResponseEntity.ok(ApiResponse.success("取得購物車成功", cartItems));
    }

    @Operation(summary = "加入購物車", description = "將指定商品加入購物車;若商品已在購物車中則累加數量")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "加入成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求參數錯誤 (商品 ID 為空或數量 < 1) ")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "商品不存在")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addToCart(
            @Parameter(hidden = true) @AuthenticationPrincipal String email,
            @Valid @RequestBody AddToCartRequest request) {
        cartService.addToCart(email, request);
        return ResponseEntity.ok(ApiResponse.success("加入購物車成功"));
    }

    @Operation(summary = "移除購物車項目", description = "依購物車項目 ID 移除單一項目;只能移除自己購物車內的項目")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "移除成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "嘗試移除他人的購物車項目")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "購物車項目不存在")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCartItem(
            @Parameter(hidden = true) @AuthenticationPrincipal String email,
            @Parameter(description = "購物車項目 ID") @PathVariable Long id) {
        cartService.removeFromCart(email, id);
        return ResponseEntity.ok(ApiResponse.success("移除商品成功"));
    }
}
