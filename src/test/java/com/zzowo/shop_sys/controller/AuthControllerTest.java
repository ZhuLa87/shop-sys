package com.zzowo.shop_sys.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzowo.shop_sys.config.SecurityConfig;
import com.zzowo.shop_sys.dto.request.auth.TokenRefreshRequest;
import com.zzowo.shop_sys.dto.request.user.UserLoginRequest;
import com.zzowo.shop_sys.dto.response.auth.LoginResponse;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.Role;
import com.zzowo.shop_sys.exception.BusinessException;
import com.zzowo.shop_sys.repository.UserRepository;
import com.zzowo.shop_sys.service.RefreshTokenService;
import com.zzowo.shop_sys.service.TokenBlacklistService;
import com.zzowo.shop_sys.service.UserService;
import com.zzowo.shop_sys.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    UserService userService;

    @MockitoBean
    RefreshTokenService refreshTokenService;

    @MockitoBean
    TokenBlacklistService tokenBlacklistService;

    // JwtAuthenticationFilter 依賴 JwtUtil,需要 MockitoBean 防止 NPE
    @MockitoBean
    JwtUtil jwtUtil;

    @MockitoBean
    UserRepository userRepository;

    // ── POST /v1/auth/login ──────────────────────────────────────────────────

    @Test
    void login_validCredentials_returns200WithBothTokens() throws Exception {
        LoginResponse loginResponse = new LoginResponse(
                "access-token", "refresh-token", "Bearer", 1800L);
        when(userService.login(any())).thenReturn(loginResponse);

        mockMvc.perform(post("/v1/auth/login")
.contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(1800));
    }

    @Test
    void login_serviceThrowsException_returns500() throws Exception {
        when(userService.login(any())).thenThrow(new RuntimeException("帳號不存在"));

        mockMvc.perform(post("/v1/auth/login")
.contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest())))
                .andExpect(status().isInternalServerError());
    }

    // ── POST /v1/auth/refresh ────────────────────────────────────────────────

    @Test
    void refresh_validRefreshToken_returns200WithNewTokens() throws Exception {
        User user = mockUser();
        when(refreshTokenService.validateAndDelete("valid-rt")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user)).thenReturn("new-access-token");
        when(refreshTokenService.create(1L)).thenReturn("new-refresh-token");
        when(jwtUtil.getAccessExpirationSeconds()).thenReturn(1800L);

        mockMvc.perform(post("/v1/auth/refresh")
.contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest("valid-rt"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void refresh_invalidRefreshToken_returns4xx() throws Exception {
        when(refreshTokenService.validateAndDelete("expired-rt"))
                .thenThrow(new BusinessException("無效或已過期的 Refresh Token,請重新登入"));

        mockMvc.perform(post("/v1/auth/refresh")
.contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest("expired-rt"))))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value("無效或已過期的 Refresh Token,請重新登入"));
    }

    @Test
    void refresh_blankRefreshToken_returns400() throws Exception {
        TokenRefreshRequest body = new TokenRefreshRequest();
        // refreshToken 留空,觸發 @NotBlank 驗證

        mockMvc.perform(post("/v1/auth/refresh")
.contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── POST /v1/auth/logout ─────────────────────────────────────────────────

    @Test
    void logout_withRefreshToken_blacklistsAccessTokenAndDeletesRefreshToken() throws Exception {
        doNothing().when(tokenBlacklistService).blacklist(anyString());
        doNothing().when(refreshTokenService).deleteIfExists(anyString());

        mockMvc.perform(post("/v1/auth/logout")
.header(HttpHeaders.AUTHORIZATION, "Bearer some-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest("some-rt"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("已成功登出"));

        verify(tokenBlacklistService).blacklist("some-access-token");
        verify(refreshTokenService).deleteIfExists("some-rt");
    }

    @Test
    void logout_withoutAuthorizationHeader_stillDeletesRefreshToken() throws Exception {
        mockMvc.perform(post("/v1/auth/logout")
.contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest("some-rt"))))
                .andExpect(status().isOk());

        verify(tokenBlacklistService, never()).blacklist(anyString());
        verify(refreshTokenService).deleteIfExists("some-rt");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UserLoginRequest loginRequest() {
        UserLoginRequest req = new UserLoginRequest();
        req.setEmail("user@test.com");
        req.setPassword("password123");
        return req;
    }

    private TokenRefreshRequest refreshRequest(String token) {
        TokenRefreshRequest req = new TokenRefreshRequest();
        req.setRefreshToken(token);
        return req;
    }

    private User mockUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");
        user.setPasswordHash("hash");
        user.setRole(Role.CUSTOMER);
        return user;
    }
}
