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

import com.zzowo.shop_sys.dto.request.user.UserLoginRequest;
import com.zzowo.shop_sys.dto.response.auth.LoginResponse;
import com.zzowo.shop_sys.dto.response.auth.TokenRefreshResponse;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.exception.BusinessException;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.repository.UserRepository;
import com.zzowo.shop_sys.util.JwtUtil;

import lombok.RequiredArgsConstructor;

// Token 的發放, 刷新與撤銷
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;

    @Transactional
    public LoginResponse login(UserLoginRequest request) {
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

        return new LoginResponse(accessToken, refreshToken, JwtUtil.TOKEN_TYPE, jwtUtil.getAccessExpirationSeconds());
    }

    // Token Rotation: 舊的 Refresh Token 立即失效, 換發新的一組
    @Transactional(readOnly = true)
    public TokenRefreshResponse refresh(String refreshToken) {
        Long userId = refreshTokenService.validateAndDelete(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("使用者不存在"));

        assertAccountActive(user);

        String newAccessToken = jwtUtil.generateToken(user);
        String newRefreshToken = refreshTokenService.create(userId);

        return new TokenRefreshResponse(newAccessToken, newRefreshToken, JwtUtil.TOKEN_TYPE, jwtUtil.getAccessExpirationSeconds());
    }

    // accessToken 可為 null (請求沒有帶 Authorization header 時), 此時只刪除 Refresh Token
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null) {
            tokenBlacklistService.blacklist(accessToken);
        }
        refreshTokenService.deleteIfExists(refreshToken);
    }

    // 換發 token 前檢查帳號狀態, 避免帳號被停用/鎖定後仍能無限期換發 access token
    void assertAccountActive(User user) {
        if (!user.isEnabled() || !user.isAccountNonLocked()) {
            throw new BusinessException("帳號已被停用或鎖定,請聯繫客服", HttpStatus.LOCKED);
        }
    }
}
