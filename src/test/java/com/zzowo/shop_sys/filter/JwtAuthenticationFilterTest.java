package com.zzowo.shop_sys.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.Role;
import com.zzowo.shop_sys.service.TokenBlacklistService;
import com.zzowo.shop_sys.util.JwtUtil;

// 使用真的 JwtUtil 簽發與驗證 token, 只 mock 黑名單 (Redis)
class JwtAuthenticationFilterTest {

    private static final String SECRET = "testSecretKeyForShopSYSThatIsLongEnoughForHS512Algorithm";

    JwtUtil jwtUtil;
    TokenBlacklistService tokenBlacklistService;
    JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 1800000L);
        ReflectionTestUtils.invokeMethod(jwtUtil, "init");
        tokenBlacklistService = mock(TokenBlacklistService.class);
        filter = new JwtAuthenticationFilter(jwtUtil, tokenBlacklistService);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validToken_setsAuthenticationWithEmailAndRole() throws Exception {
        String token = jwtUtil.generateToken(user(Role.PRODUCT_MANAGER));

        doFilter("Bearer " + token);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("pm@test.com");
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_PRODUCT_MANAGER");
    }

    @Test
    void blacklistedToken_isNotAuthenticated() throws Exception {
        String token = jwtUtil.generateToken(user(Role.CUSTOMER));
        when(tokenBlacklistService.isBlacklistedJti(jwtUtil.getJtiFromToken(token))).thenReturn(true);

        doFilter("Bearer " + token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void tamperedToken_isNotAuthenticated_andSkipsBlacklistLookup() throws Exception {
        String token = jwtUtil.generateToken(user(Role.CUSTOMER));
        // 改掉簽章的第一個字元 (末字元含有 base64 補位 bit, 改了不一定會影響簽章)
        int sigStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(sigStart) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, sigStart) + replacement + token.substring(sigStart + 1);

        doFilter("Bearer " + tampered);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenBlacklistService, never()).isBlacklistedJti(any());
    }

    @Test
    void expiredToken_isNotAuthenticated() throws Exception {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -10000L);
        String token = jwtUtil.generateToken(user(Role.CUSTOMER));

        doFilter("Bearer " + token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void missingOrNonBearerHeader_isNotAuthenticated() throws Exception {
        doFilter(null);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        doFilter("Basic dXNlcjpwYXNz");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private void doFilter(String authorization) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/users/me");
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        assertThat(chain.getRequest()).isNotNull(); // 不論認證與否都要繼續往下
    }

    private User user(Role role) {
        User user = new User();
        user.setId(1L);
        user.setEmail(role == Role.PRODUCT_MANAGER ? "pm@test.com" : "user@test.com");
        user.setRole(role);
        return user;
    }
}
