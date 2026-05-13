package com.zzowo.shop_sys.controller;

import com.zzowo.shop_sys.dto.request.auth.TokenRefreshRequest;
import com.zzowo.shop_sys.dto.request.user.UserLoginRequest;
import com.zzowo.shop_sys.dto.request.user.UserRegisterRequest;
import com.zzowo.shop_sys.dto.response.ApiResponse;
import com.zzowo.shop_sys.dto.response.auth.LoginResponse;
import com.zzowo.shop_sys.dto.response.auth.TokenRefreshResponse;
import com.zzowo.shop_sys.dto.response.user.RegisterResponse;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.repository.UserRepository;
import com.zzowo.shop_sys.service.RefreshTokenService;
import com.zzowo.shop_sys.service.TokenBlacklistService;
import com.zzowo.shop_sys.service.UserService;
import com.zzowo.shop_sys.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "認證相關 API（註冊、登入、Token 刷新、登出）")
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Operation(summary = "註冊新帳號", description = "建立新使用者帳號，預設角色為 CUSTOMER")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "註冊成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求參數錯誤（Email 格式、密碼長度不符）")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email 已被使用")
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody UserRegisterRequest request) {
        RegisterResponse newUser = userService.register(request);
        return ResponseEntity.ok(ApiResponse.success("註冊成功", newUser));
    }

    @Operation(summary = "登入", description = "使用 Email + 密碼登入，成功後回傳 Access Token 與 Refresh Token")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登入成功，回傳 Token 資訊")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求參數錯誤")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Email 或密碼錯誤")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "423", description = "帳號已被鎖定")
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody UserLoginRequest request) {
        LoginResponse loginResponse = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success("登入成功", loginResponse));
    }

    @Operation(summary = "刷新 Token", description = "使用 Refresh Token 換發新的 Access Token 與 Refresh Token（Token Rotation，舊 Refresh Token 立即失效）")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token 刷新成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求參數錯誤")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh Token 無效或已過期")
    @SecurityRequirements
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        Long userId = refreshTokenService.validateAndDelete(request.getRefreshToken());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("使用者不存在"));

        String newAccessToken = jwtUtil.generateToken(user);
        String newRefreshToken = refreshTokenService.create(userId);

        TokenRefreshResponse body = new TokenRefreshResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                jwtUtil.getAccessExpirationSeconds()
        );
        return ResponseEntity.ok(ApiResponse.success("Token 已刷新", body));
    }

    @Operation(summary = "登出", description = "將目前的 Access Token 加入黑名單，並同步刪除 Refresh Token")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登出成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未登入或 Token 無效")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody TokenRefreshRequest request,
            HttpServletRequest httpRequest) {

        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            tokenBlacklistService.blacklist(authHeader.substring(7));
        }

        refreshTokenService.deleteIfExists(request.getRefreshToken());

        return ResponseEntity.ok(ApiResponse.success("已成功登出", null));
    }
}
