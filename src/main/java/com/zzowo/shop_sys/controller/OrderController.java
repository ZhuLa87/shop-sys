package com.zzowo.shop_sys.controller;

import com.zzowo.shop_sys.dto.request.order.OrderCreateRequest;
import com.zzowo.shop_sys.dto.response.ApiResponse;
import com.zzowo.shop_sys.dto.response.order.OrderResponse;
import com.zzowo.shop_sys.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 建立訂單 (結帳)
     * URL: POST /v1/orders
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody OrderCreateRequest request) {

        OrderResponse order = orderService.createOrder(email, request);
        return ResponseEntity.ok(ApiResponse.success("訂單建立成功", order));
    }

    /**
     * 查詢我的所有訂單
     * URL: GET /v1/orders
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal String email) {

        List<OrderResponse> orders = orderService.getMyOrders(email);
        return ResponseEntity.ok(ApiResponse.success("取得訂單列表成功", orders));
    }

    /**
     * 查詢特定訂單詳情
     * URL: GET /v1/orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @AuthenticationPrincipal String email,
            @PathVariable Long id) {

        OrderResponse order = orderService.getOrderById(email, id);
        return ResponseEntity.ok(ApiResponse.success("取得訂單詳情成功", order));
    }
}