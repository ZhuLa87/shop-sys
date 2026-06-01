package com.zzowo.shop_sys.controller;

import com.zzowo.shop_sys.config.SecurityConfig;
import com.zzowo.shop_sys.dto.response.product.ProductResponse;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.repository.UserRepository;
import com.zzowo.shop_sys.service.ProductService;
import com.zzowo.shop_sys.service.TokenBlacklistService;
import com.zzowo.shop_sys.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ProductService productService;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenBlacklistService tokenBlacklistService;

    // ── GET /v1/products/deleted ─────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "PRODUCT_MANAGER")
    void getDeletedProducts_productManager_returns200WithList() throws Exception {
        ProductResponse r = new ProductResponse();
        r.setId(1L);
        r.setName("已刪除商品");
        r.setPrice(BigDecimal.valueOf(100));
        when(productService.getDeletedProducts()).thenReturn(List.of(r));

        mockMvc.perform(get("/v1/products/deleted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("已刪除商品"));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void getDeletedProducts_superAdmin_returns200() throws Exception {
        when(productService.getDeletedProducts()).thenReturn(List.of());

        mockMvc.perform(get("/v1/products/deleted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getDeletedProducts_customer_returns403() throws Exception {
        mockMvc.perform(get("/v1/products/deleted"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void getDeletedProducts_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/v1/products/deleted"))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /v1/products/{id}/restore ────────────────────────────────────────

    @Test
    @WithMockUser(roles = "PRODUCT_MANAGER")
    void restoreProduct_productManager_returns200WithRestoredProduct() throws Exception {
        ProductResponse restored = new ProductResponse();
        restored.setId(1L);
        restored.setName("已還原商品");
        when(productService.restoreProduct(1L)).thenReturn(restored);

        mockMvc.perform(put("/v1/products/1/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("已還原商品"));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void restoreProduct_superAdmin_returns200() throws Exception {
        when(productService.restoreProduct(2L)).thenReturn(new ProductResponse());

        mockMvc.perform(put("/v1/products/2/restore"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void restoreProduct_customer_returns403() throws Exception {
        mockMvc.perform(put("/v1/products/1/restore"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void restoreProduct_anonymous_returns401() throws Exception {
        mockMvc.perform(put("/v1/products/1/restore"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "PRODUCT_MANAGER")
    void restoreProduct_notFound_returns404WithMessage() throws Exception {
        when(productService.restoreProduct(99L))
                .thenThrow(new ResourceNotFoundException("找不到已刪除的商品 ID: 99"));

        mockMvc.perform(put("/v1/products/99/restore"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("找不到已刪除的商品 ID: 99"));
    }
}
