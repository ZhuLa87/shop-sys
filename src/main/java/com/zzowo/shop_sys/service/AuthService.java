package com.zzowo.shop_sys.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.zzowo.shop_sys.dto.request.user.UserLoginRequest;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.event.UserSessionsRevokedEvent;
import com.zzowo.shop_sys.exception.BusinessException;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.repository.UserRepository;
import com.zzowo.shop_sys.util.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Token 的發放, 刷新與撤銷
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;

    @Transactional
    public IssuedTokens login(UserLoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BusinessException("帳號或密碼錯誤", HttpStatus.UNAUTHORIZED);
        } catch (DisabledException | LockedException e) {
            throw new BusinessException("帳號已被停用或鎖定,請聯繫客服", HttpStatus.LOCKED);
        }

        User user = (User) authentication.getPrincipal();
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = refreshTokenService.create(user.getId());

        return new IssuedTokens(accessToken, refreshToken, jwtUtil.getAccessExpirationSeconds());
    }

    // Token Rotation: 舊的 Refresh Token 立即失效, 換發新的一組
    @Transactional(readOnly = true)
    public IssuedTokens refresh(String refreshToken) {
        Long userId = refreshTokenService.validateAndDelete(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("使用者不存在"));

        assertAccountActive(user);

        String newAccessToken = jwtUtil.generateToken(user);
        String newRefreshToken = refreshTokenService.create(userId);

        return new IssuedTokens(newAccessToken, newRefreshToken, jwtUtil.getAccessExpirationSeconds());
    }

    // 兩者都可為 null (沒有 Authorization header / 沒有 refresh_token cookie), 只處理有帶的那個
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null) {
            tokenBlacklistService.blacklist(accessToken);
        }
        if (refreshToken != null) {
            refreshTokenService.deleteIfExists(refreshToken);
        }
    }

    // 帳號異動後讓該使用者已發出的 access token 與 refresh token 全部失效.
    // 必須在 commit 之後: 若先撤銷, 撤銷到 commit 之間的 refresh 會讀到舊狀態 (例如仍是啟用),
    // 發出一個晚於撤銷時間的新 token 而漏網. commit 後才撤銷, 中間換發的 token 一定早於撤銷時間
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserSessionsRevoked(UserSessionsRevokedEvent event) {
        try {
            tokenBlacklistService.revokeAllForUser(event.userId());
            refreshTokenService.revokeForUser(event.userId());
        } catch (RuntimeException e) {
            // 資料庫變更已經 commit, 不能再回報失敗; 停用帳號仍有 refresh 時的 assertAccountActive 擋著
            log.error("撤銷使用者 {} 的 token 失敗: {}", event.userId(), e.getMessage(), e);
        }
    }

    // 換發 token 前檢查帳號狀態, 避免帳號被停用/鎖定後仍能無限期換發 access token
    void assertAccountActive(User user) {
        if (!user.isEnabled() || !user.isAccountNonLocked()) {
            throw new BusinessException("帳號已被停用或鎖定,請聯繫客服", HttpStatus.LOCKED);
        }
    }
}
