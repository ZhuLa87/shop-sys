package com.zzowo.shop_sys.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzowo.shop_sys.config.SecurityConfig;
import com.zzowo.shop_sys.dto.request.user.AdminUpdateUserRequest;
import com.zzowo.shop_sys.dto.request.user.UserSelfUpdateRequest;
import com.zzowo.shop_sys.dto.response.user.UserResponse;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.repository.UserRepository;
import com.zzowo.shop_sys.service.TokenBlacklistService;
import com.zzowo.shop_sys.service.UserService;
import com.zzowo.shop_sys.util.JwtUtil;

/**
 * 這層測試的重點是 SecurityConfig 中手寫的角色規則 (/v1/users/me 登入即可,
 * /v1/users/{id} 與 /v1/users 僅限 SUPER_ADMIN) ,避免規則被改動時靜默失效.
 */
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean UserService userService;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean UserRepository userRepository;
    @MockitoBean TokenBlacklistService tokenBlacklistService;

    // ── GET /v1/users/me ─────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getCurrentUser_authenticated_returns200() throws Exception {
        UserResponse response = new UserResponse();
        response.setId(1L);
        response.setEmail("user@test.com");
        when(userService.getUserProfile(any())).thenReturn(response);

        mockMvc.perform(get("/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("user@test.com"));
    }

    @Test
    @WithAnonymousUser
    void getCurrentUser_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /v1/users/{id} (SUPER_ADMIN only) ────────────────────────────────

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void getUserById_superAdmin_returns200() throws Exception {
        when(userService.getUserById(2L)).thenReturn(new UserResponse());

        mockMvc.perform(get("/v1/users/2"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getUserById_customer_returns403() throws Exception {
        mockMvc.perform(get("/v1/users/2"))
                .andExpect(status().isForbidden());
        verify(userService, never()).getUserById(any());
    }

    @Test
    @WithMockUser(roles = "PRODUCT_MANAGER")
    void getUserById_productManager_returns403() throws Exception {
        mockMvc.perform(get("/v1/users/2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void getUserById_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/v1/users/2"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void getUserById_notFound_returns404() throws Exception {
        when(userService.getUserById(99L)).thenThrow(new ResourceNotFoundException("找不到使用者 ID: 99"));

        mockMvc.perform(get("/v1/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("找不到使用者 ID: 99"));
    }

    // ── GET /v1/users (SUPER_ADMIN only) ─────────────────────────────────────

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void getAllUsers_superAdmin_returns200WithList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(new UserResponse(), new UserResponse()));

        mockMvc.perform(get("/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getAllUsers_customer_returns403() throws Exception {
        mockMvc.perform(get("/v1/users"))
                .andExpect(status().isForbidden());
        verify(userService, never()).getAllUsers();
    }

    // ── PUT /v1/users/me ─────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateMyProfile_authenticated_returns200() throws Exception {
        UserSelfUpdateRequest request = new UserSelfUpdateRequest();
        request.setName("新名字");

        mockMvc.perform(put("/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userService).updateMyInfo(any(), any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateMyProfile_invalidEmailFormat_returns422() throws Exception {
        UserSelfUpdateRequest request = new UserSelfUpdateRequest();
        request.setEmail("not-an-email");

        mockMvc.perform(put("/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());

        verify(userService, never()).updateMyInfo(any(), any());
    }

    @Test
    @WithAnonymousUser
    void updateMyProfile_anonymous_returns401() throws Exception {
        mockMvc.perform(put("/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserSelfUpdateRequest())))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /v1/users/{id} (SUPER_ADMIN only) ────────────────────────────────

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void updateUserByAdmin_superAdmin_returns200() throws Exception {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setRole("PRODUCT_MANAGER");

        mockMvc.perform(put("/v1/users/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userService).updateUserByAdmin(any(), any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void updateUserByAdmin_customer_returns403() throws Exception {
        mockMvc.perform(put("/v1/users/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminUpdateUserRequest())))
                .andExpect(status().isForbidden());

        verify(userService, never()).updateUserByAdmin(any(), any());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void updateUserByAdmin_userNotFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("找不到使用者 ID: 99"))
                .when(userService).updateUserByAdmin(any(), any());

        mockMvc.perform(put("/v1/users/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminUpdateUserRequest())))
                .andExpect(status().isNotFound());
    }
}
