package com.zzowo.shop_sys.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzowo.shop_sys.config.SecurityConfig;
import com.zzowo.shop_sys.dto.request.user.UserLoginRequest;
import com.zzowo.shop_sys.exception.BusinessException;
import com.zzowo.shop_sys.repository.UserRepository;
import com.zzowo.shop_sys.service.AuthService;
import com.zzowo.shop_sys.service.IssuedTokens;
import com.zzowo.shop_sys.service.TokenBlacklistService;
import com.zzowo.shop_sys.service.UserService;
import com.zzowo.shop_sys.util.JwtUtil;
import com.zzowo.shop_sys.util.RefreshTokenCookie;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, RefreshTokenCookie.class})
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    UserService userService;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    JwtUtil jwtUtil;

    @MockitoBean
    UserRepository userRepository;

    // ── POST /v1/auth/login ──────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsAccessTokenInBodyAndRefreshTokenInHttpOnlyCookie() throws Exception {
        when(authService.login(any())).thenReturn(new IssuedTokens("access-token", "refresh-token", 1800L));

        mockMvc.perform(post("/v1/auth/login")
.contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(1800))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        containsString("refresh_token=refresh-token"),
                        containsString("Path=/"),
                        containsString("Secure"),
                        containsString("HttpOnly"),
                        containsString("SameSite=Lax"))));
    }

    @Test
    void login_blankEmail_returns422() throws Exception {
        UserLoginRequest request = loginRequest();
        request.setEmail("");

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());

        verify(authService, never()).login(any());
    }

    @Test
    void login_serviceThrowsException_returns500() throws Exception {
        when(authService.login(any())).thenThrow(new RuntimeException("帳號不存在"));

        mockMvc.perform(post("/v1/auth/login")
.contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest())))
                .andExpect(status().isInternalServerError());
    }

    // ── POST /v1/auth/refresh ────────────────────────────────────────────────

    @Test
    void refresh_validCookie_returns200AndRotatesCookie() throws Exception {
        when(authService.refresh("valid-rt")).thenReturn(
                new IssuedTokens("new-access-token", "new-refresh-token", 1800L));

        mockMvc.perform(post("/v1/auth/refresh")
                        .cookie(new Cookie(RefreshTokenCookie.NAME, "valid-rt")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        containsString("refresh_token=new-refresh-token"),
                        containsString("HttpOnly"))));
    }

    @Test
    void refresh_invalidCookie_returns4xxAndClearsCookie() throws Exception {
        when(authService.refresh("expired-rt"))
                .thenThrow(new BusinessException("無效或已過期的 Refresh Token,請重新登入"));

        mockMvc.perform(post("/v1/auth/refresh")
                        .cookie(new Cookie(RefreshTokenCookie.NAME, "expired-rt")))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value("無效或已過期的 Refresh Token,請重新登入"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                        containsString("refresh_token=;"),
                        containsString("Max-Age=0"))));
    }

    @Test
    void refresh_withoutCookie_returns401() throws Exception {
        mockMvc.perform(post("/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());

        verify(authService, never()).refresh(any());
    }

    // ── POST /v1/auth/logout ─────────────────────────────────────────────────

    @Test
    void logout_withCookie_blacklistsAccessTokenDeletesRefreshTokenAndClearsCookie() throws Exception {
        mockMvc.perform(post("/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer some-access-token")
                        .cookie(new Cookie(RefreshTokenCookie.NAME, "some-rt")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("已成功登出"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        verify(authService).logout("some-access-token", "some-rt");
    }

    @Test
    void logout_withoutAuthorizationHeaderOrCookie_stillClearsCookie() throws Exception {
        mockMvc.perform(post("/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        verify(authService).logout(null, null);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UserLoginRequest loginRequest() {
        UserLoginRequest req = new UserLoginRequest();
        req.setEmail("user@test.com");
        req.setPassword("password123");
        return req;
    }
}
