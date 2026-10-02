package com.zzowo.shop_sys.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.zzowo.shop_sys.dto.request.user.UserLoginRequest;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.Role;
import com.zzowo.shop_sys.event.UserSessionsRevokedEvent;
import com.zzowo.shop_sys.exception.BusinessException;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.repository.UserRepository;
import com.zzowo.shop_sys.util.JwtUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtUtil jwtUtil;
    @Mock RefreshTokenService refreshTokenService;
    @Mock TokenBlacklistService tokenBlacklistService;
    @InjectMocks AuthService authService;

    // ── login ────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsTokens() {
        User user = buildUser(true, true);
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(jwtUtil.generateToken(user)).thenReturn("access-token");
        when(jwtUtil.getAccessExpirationSeconds()).thenReturn(1800L);
        when(refreshTokenService.create(1L)).thenReturn("refresh-token");

        IssuedTokens tokens = authService.login(loginRequest());

        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(tokens.refreshToken()).isEqualTo("refresh-token");
        assertThat(tokens.expiresIn()).isEqualTo(1800L);
        assertThat(user.getLastLoginAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void login_wrongPassword_throws401() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("帳號或密碼錯誤")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(refreshTokenService, never()).create(any());
    }

    @Test
    void login_disabledAccount_throws423() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));

        assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.LOCKED);
    }

    @Test
    void login_lockedAccount_throws423() {
        when(authenticationManager.authenticate(any())).thenThrow(new LockedException("locked"));

        assertThatThrownBy(() -> authService.login(loginRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.LOCKED);
    }

    // ── refresh ──────────────────────────────────────────────────────────────

    @Test
    void refresh_validToken_rotatesTokens() {
        User user = buildUser(true, true);
        when(refreshTokenService.validateAndDelete("old-rt")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user)).thenReturn("new-access-token");
        when(refreshTokenService.create(1L)).thenReturn("new-refresh-token");
        when(jwtUtil.getAccessExpirationSeconds()).thenReturn(1800L);

        IssuedTokens tokens = authService.refresh("old-rt");

        assertThat(tokens.accessToken()).isEqualTo("new-access-token");
        assertThat(tokens.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(tokens.expiresIn()).isEqualTo(1800L);
    }

    @Test
    void refresh_disabledUser_throws423_andIssuesNoToken() {
        when(refreshTokenService.validateAndDelete("old-rt")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser(false, true)));

        assertThatThrownBy(() -> authService.refresh("old-rt"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.LOCKED);

        verify(refreshTokenService, never()).create(any());
    }

    @Test
    void refresh_userNotFound_throwsResourceNotFoundException() {
        when(refreshTokenService.validateAndDelete("old-rt")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("old-rt"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── logout ───────────────────────────────────────────────────────────────

    @Test
    void logout_withAccessToken_blacklistsItAndDeletesRefreshToken() {
        authService.logout("access-token", "rt");

        verify(tokenBlacklistService).blacklist("access-token");
        verify(refreshTokenService).deleteIfExists("rt");
    }

    @Test
    void logout_withoutAccessToken_onlyDeletesRefreshToken() {
        authService.logout(null, "rt");

        verify(tokenBlacklistService, never()).blacklist(any());
        verify(refreshTokenService).deleteIfExists("rt");
    }

    @Test
    void logout_withoutRefreshToken_onlyBlacklistsAccessToken() {
        authService.logout("access-token", null);

        verify(tokenBlacklistService).blacklist("access-token");
        verify(refreshTokenService, never()).deleteIfExists(any());
    }

    // ── onUserSessionsRevoked ────────────────────────────────────────────────

    @Test
    void onUserSessionsRevoked_revokesAccessAndRefreshTokens() {
        authService.onUserSessionsRevoked(new UserSessionsRevokedEvent(1L));

        verify(tokenBlacklistService).revokeAllForUser(1L);
        verify(refreshTokenService).revokeForUser(1L);
    }

    @Test
    void onUserSessionsRevoked_redisFailure_doesNotPropagate() {
        // 資料庫變更已經 commit, 撤銷失敗只記錄, 不讓請求回報失敗
        doThrow(new RuntimeException("connection refused")).when(tokenBlacklistService).revokeAllForUser(1L);

        authService.onUserSessionsRevoked(new UserSessionsRevokedEvent(1L));
    }

    // ── assertAccountActive ──────────────────────────────────────────────────

    @Test
    void assertAccountActive_lockedUser_throws423() {
        assertThatThrownBy(() -> authService.assertAccountActive(buildUser(true, false)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.LOCKED);
    }

    @Test
    void assertAccountActive_activeUser_passes() {
        authService.assertAccountActive(buildUser(true, true));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User buildUser(boolean enabled, boolean accountNonLocked) {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");
        user.setPasswordHash("hashed");
        user.setRole(Role.CUSTOMER);
        user.setEnabled(enabled);
        user.setAccountNonLocked(accountNonLocked);
        return user;
    }

    private UserLoginRequest loginRequest() {
        UserLoginRequest request = new UserLoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("password123");
        return request;
    }
}
