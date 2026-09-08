package com.zzowo.shop_sys.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzowo.shop_sys.config.SecurityConfig;
import com.zzowo.shop_sys.dto.request.order.OrderCreateRequest;
import com.zzowo.shop_sys.dto.response.order.OrderResponse;
import com.zzowo.shop_sys.enums.OrderStatus;
import com.zzowo.shop_sys.exception.BusinessException;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.repository.UserRepository;
import com.zzowo.shop_sys.service.OrderService;
import com.zzowo.shop_sys.service.TokenBlacklistService;
import com.zzowo.shop_sys.util.JwtUtil;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean OrderService orderService;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenBlacklistService tokenBlacklistService;

    // ── POST /v1/orders ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createOrder_authenticated_returns200WithOrder() throws Exception {
        OrderResponse response = new OrderResponse();
        response.setId(1001L);
        response.setStatus(OrderStatus.PENDING);
        response.setTotalAmount(BigDecimal.valueOf(14000));
        when(orderService.createOrder(any(), any())).thenReturn(response);

        mockMvc.perform(post("/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1001))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @WithAnonymousUser
    void createOrder_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isUnauthorized());

        verify(orderService, never()).createOrder(any(), any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createOrder_blankRecipientName_returns422() throws Exception {
        OrderCreateRequest request = createRequest();
        request.setRecipientName("");

        mockMvc.perform(post("/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());

        verify(orderService, never()).createOrder(any(), any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createOrder_emptyCart_returns400WithMessage() throws Exception {
        when(orderService.createOrder(any(), any()))
                .thenThrow(new BusinessException("購物車為空,無法結帳"));

        mockMvc.perform(post("/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("購物車為空,無法結帳"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void createOrder_insufficientStock_returns400WithProductName() throws Exception {
        when(orderService.createOrder(any(), any()))
                .thenThrow(new BusinessException("商品 [熱門商品] 庫存不足,結帳失敗"));

        mockMvc.perform(post("/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("商品 [熱門商品] 庫存不足,結帳失敗"));
    }

    // ── GET /v1/orders ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getMyOrders_authenticated_returns200WithList() throws Exception {
        when(orderService.getMyOrders(any())).thenReturn(List.of(new OrderResponse(), new OrderResponse()));

        mockMvc.perform(get("/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithAnonymousUser
    void getMyOrders_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/v1/orders"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /v1/orders/{id} ──────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getOrder_ownOrder_returns200() throws Exception {
        OrderResponse response = new OrderResponse();
        response.setId(50L);
        when(orderService.getOrderById(any(), any())).thenReturn(response);

        mockMvc.perform(get("/v1/orders/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(50));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getOrder_otherUsersOrder_returns403() throws Exception {
        when(orderService.getOrderById(any(), any()))
                .thenThrow(new BusinessException("無權限查看此訂單", HttpStatus.FORBIDDEN));

        mockMvc.perform(get("/v1/orders/50"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("無權限查看此訂單"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getOrder_notFound_returns404() throws Exception {
        when(orderService.getOrderById(any(), any()))
                .thenThrow(new ResourceNotFoundException("訂單不存在"));

        mockMvc.perform(get("/v1/orders/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithAnonymousUser
    void getOrder_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/v1/orders/50"))
                .andExpect(status().isUnauthorized());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private OrderCreateRequest createRequest() {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setRecipientName("王小明");
        request.setRecipientPhone("0912345678");
        request.setRecipientAddress("台北市中正區忠孝東路一段 1 號");
        return request;
    }
}
