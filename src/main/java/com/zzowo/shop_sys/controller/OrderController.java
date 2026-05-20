package com.zzowo.shop_sys.controller;

import com.zzowo.shop_sys.dto.request.order.OrderCreateRequest;
import com.zzowo.shop_sys.dto.response.ApiResponse;
import com.zzowo.shop_sys.dto.response.order.OrderResponse;
import com.zzowo.shop_sys.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Order", description = "訂單 API (需登入,僅能操作自己的訂單) ")
@RestController
@RequestMapping("/v1/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Operation(summary = "建立訂單 (結帳) ", description = "從目前使用者的購物車建立訂單並扣減庫存,訂單初始狀態為 PENDING (待付款) ")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "訂單建立成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求參數錯誤 (必填欄位為空) ")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "庫存不足,無法完成下單")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Parameter(hidden = true) @AuthenticationPrincipal String email,
            @Valid @RequestBody OrderCreateRequest request) {
        OrderResponse order = orderService.createOrder(email, request);
        return ResponseEntity.ok(ApiResponse.success("訂單建立成功", order));
    }

    @Operation(summary = "查詢我的所有訂單", description = "取得目前登入使用者的所有訂單列表,依建立時間降序排列")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取得成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入")
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @Parameter(hidden = true) @AuthenticationPrincipal String email) {
        List<OrderResponse> orders = orderService.getMyOrders(email);
        return ResponseEntity.ok(ApiResponse.success("取得訂單列表成功", orders));
    }

    @Operation(summary = "查詢特定訂單詳情", description = "依訂單 ID 取得訂單詳情 (含明細) ;只能查詢自己的訂單")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "取得成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "嘗試查詢他人的訂單")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "訂單不存在")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @Parameter(hidden = true) @AuthenticationPrincipal String email,
            @Parameter(description = "訂單 ID") @PathVariable Long id) {
        OrderResponse order = orderService.getOrderById(email, id);
        return ResponseEntity.ok(ApiResponse.success("取得訂單詳情成功", order));
    }
}
