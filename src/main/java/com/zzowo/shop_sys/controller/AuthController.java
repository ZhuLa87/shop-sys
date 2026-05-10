package com.zzowo.shop_sys.controller;

import com.zzowo.shop_sys.dto.request.auth.TokenRefreshRequest;
import com.zzowo.shop_sys.dto.request.user.UserLoginRequest;
import com.zzowo.shop_sys.dto.request.user.UserRegisterRequest;
import com.zzowo.shop_sys.dto.response.ApiResponse;
import com.zzowo.shop_sys.dto.response.auth.LoginResponse;
import com.zzowo.shop_sys.dto.response.auth.TokenRefreshResponse;
import com.zzowo.shop_sys.dto.response.user.RegisterResponse;
import com.zzowo.shop_sys.entity.RefreshToken;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.repository.UserRepository;
import com.zzowo.shop_sys.service.RefreshTokenService;
import com.zzowo.shop_sys.service.UserService;
import com.zzowo.shop_sys.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    // POST /api/v1/auth/register
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody UserRegisterRequest request) {
        RegisterResponse newUser = userService.register(request);
        return ResponseEntity.ok(ApiResponse.success("註冊成功", newUser));
    }

    // POST /api/v1/auth/login
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody UserLoginRequest request) {
        LoginResponse loginResponse = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success("登入成功", loginResponse));
    }

    // POST /api/v1/auth/refresh
    // 用 Refresh Token 換發新的 Access Token（並輪替 Refresh Token）
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        RefreshToken old = refreshTokenService.validateAndRotate(request.getRefreshToken());

        User user = userRepository.findById(old.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("使用者不存在"));

        String newAccessToken = jwtUtil.generateToken(user);
        RefreshToken newRefreshToken = refreshTokenService.create(user);

        TokenRefreshResponse body = new TokenRefreshResponse(
                newAccessToken,
                newRefreshToken.getToken(),
                "Bearer",
                jwtUtil.getAccessExpirationSeconds()
        );
        return ResponseEntity.ok(ApiResponse.success("Token 已刷新", body));
    }

    // POST /api/v1/auth/logout
    // 傳入 Refresh Token，登出後使其失效
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody TokenRefreshRequest request) {
        RefreshToken rt = refreshTokenService.validateAndRotate(request.getRefreshToken());
        // validateAndRotate 已刪除 token，登出完成
        return ResponseEntity.ok(ApiResponse.success("已成功登出", null));
    }
}
