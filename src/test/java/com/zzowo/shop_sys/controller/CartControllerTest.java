package com.zzowo.shop_sys.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.zzowo.shop_sys.dto.request.cart.AddToCartRequest;
import com.zzowo.shop_sys.dto.response.cart.CartItemResponse;
import com.zzowo.shop_sys.exception.BusinessException;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.repository.UserRepository;
import com.zzowo.shop_sys.service.CartService;
import com.zzowo.shop_sys.service.TokenBlacklistService;
import com.zzowo.shop_sys.util.JwtUtil;

@WebMvcTest(CartController.class)
@Import(SecurityConfig.class)
class CartControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean CartService cartService;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenBlacklistService tokenBlacklistService;

    // ── GET /v1/carts ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getMyCart_authenticated_returns200WithItems() throws Exception {
        CartItemResponse item = new CartItemResponse();
        item.setId(1L);
        item.setProductName("測試商品");
        item.setSubtotal(BigDecimal.valueOf(200));
        when(cartService.getUserCart(any())).thenReturn(List.of(item));

        mockMvc.perform(get("/v1/carts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].productName").value("測試商品"));
    }

    @Test
    @WithAnonymousUser
    void getMyCart_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/v1/carts"))
                .andExpect(status().isUnauthorized());

        verify(cartService, never()).getUserCart(any());
    }

    // ── POST /v1/carts ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void addToCart_validRequest_returns200() throws Exception {
        mockMvc.perform(post("/v1/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest(10L, 2))))
                .andExpect(status().isOk());

        verify(cartService).addToCart(any(), any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void addToCart_quantityBelowOne_returns422() throws Exception {
        mockMvc.perform(post("/v1/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest(10L, 0))))
                .andExpect(status().isUnprocessableEntity());

        verify(cartService, never()).addToCart(any(), any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void addToCart_missingProductId_returns422() throws Exception {
        mockMvc.perform(post("/v1/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest(null, 1))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void addToCart_insufficientStock_returns400WithMessage() throws Exception {
        doThrow(new BusinessException("庫存不足,目前購物車內已有 8 件,無法再加入 3 件"))
                .when(cartService).addToCart(any(), any());

        mockMvc.perform(post("/v1/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest(10L, 3))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("庫存不足,目前購物車內已有 8 件,無法再加入 3 件"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void addToCart_productNotFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("商品不存在"))
                .when(cartService).addToCart(any(), any());

        mockMvc.perform(post("/v1/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest(99L, 1))))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithAnonymousUser
    void addToCart_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/v1/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest(10L, 1))))
                .andExpect(status().isUnauthorized());
    }

    // ── DELETE /v1/carts/{id} ────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteCartItem_ownItem_returns200() throws Exception {
        mockMvc.perform(delete("/v1/carts/5"))
                .andExpect(status().isOk());

        verify(cartService).removeFromCart(any(), any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteCartItem_otherUsersItem_returns403() throws Exception {
        doThrow(new BusinessException("無權限操作此購物車", HttpStatus.FORBIDDEN))
                .when(cartService).removeFromCart(any(), any());

        mockMvc.perform(delete("/v1/carts/5"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("無權限操作此購物車"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void deleteCartItem_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("購物車資料不存在"))
                .when(cartService).removeFromCart(any(), any());

        mockMvc.perform(delete("/v1/carts/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithAnonymousUser
    void deleteCartItem_anonymous_returns401() throws Exception {
        mockMvc.perform(delete("/v1/carts/5"))
                .andExpect(status().isUnauthorized());

        verify(cartService, never()).removeFromCart(any(), any());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private AddToCartRequest addRequest(Long productId, Integer quantity) {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(productId);
        request.setQuantity(quantity);
        return request;
    }
}
