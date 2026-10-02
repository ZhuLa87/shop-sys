package com.zzowo.shop_sys.controller;

import lombok.RequiredArgsConstructor;
import com.zzowo.shop_sys.dto.request.user.UserLoginRequest;
import com.zzowo.shop_sys.dto.request.user.UserRegisterRequest;
import com.zzowo.shop_sys.dto.response.ApiResponse;
import com.zzowo.shop_sys.dto.response.auth.LoginResponse;
import com.zzowo.shop_sys.dto.response.auth.TokenRefreshResponse;
import com.zzowo.shop_sys.dto.response.user.RegisterResponse;
import com.zzowo.shop_sys.exception.BusinessException;
import com.zzowo.shop_sys.service.AuthService;
import com.zzowo.shop_sys.service.IssuedTokens;
import com.zzowo.shop_sys.service.UserService;
import com.zzowo.shop_sys.util.JwtUtil;
import com.zzowo.shop_sys.util.RefreshTokenCookie;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "認證相關 API (註冊,登入,Token 刷新,登出) ")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER_PREFIX = JwtUtil.TOKEN_TYPE + " ";

    private final UserService userService;

    private final AuthService authService;

    private final RefreshTokenCookie refreshTokenCookie;

    @Operation(summary = "註冊新帳號", description = "建立新使用者帳號,預設角色為 CUSTOMER")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "註冊成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求參數錯誤 (Email 格式,密碼長度不符) ")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email 已被使用")
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody UserRegisterRequest request) {
        RegisterResponse newUser = userService.register(request);
        return ResponseEntity.ok(ApiResponse.success("註冊成功", newUser));
    }

    @Operation(summary = "登入", description = "使用 Email + 密碼登入,成功後回傳 Access Token,Refresh Token 以 HttpOnly cookie (refresh_token) 核發")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登入成功,回傳 Token 資訊")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求參數錯誤")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Email 或密碼錯誤")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "423", description = "帳號已被鎖定")
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody UserLoginRequest request) {
        IssuedTokens tokens = authService.login(request);
        LoginResponse body = new LoginResponse(tokens.accessToken(), JwtUtil.TOKEN_TYPE, tokens.expiresIn());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.issue(tokens.refreshToken()).toString())
                .body(ApiResponse.success("登入成功", body));
    }

    @Operation(summary = "刷新 Token", description = "使用 refresh_token cookie 換發新的 Access Token 與 Refresh Token (Token Rotation,舊 Refresh Token 立即失效,新的以 cookie 寫回) ")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token 刷新成功")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Refresh Token 無效或已過期")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "沒有 refresh_token cookie")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "423", description = "帳號已被停用或鎖定")
    @SecurityRequirements
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken,
            HttpServletResponse httpResponse) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException("未登入或登入已過期,請重新登入", HttpStatus.UNAUTHORIZED);
        }

        IssuedTokens tokens;
        try {
            tokens = authService.refresh(refreshToken);
        } catch (RuntimeException e) {
            // 換發失敗時一併清掉已失效的 cookie, 交給 GlobalExceptionHandler 回應時這個 header 會保留
            httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear().toString());
            throw e;
        }

        TokenRefreshResponse body = new TokenRefreshResponse(tokens.accessToken(), JwtUtil.TOKEN_TYPE, tokens.expiresIn());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.issue(tokens.refreshToken()).toString())
                .body(ApiResponse.success("Token 已刷新", body));
    }

    @Operation(summary = "登出", description = "將目前的 Access Token 加入黑名單,刪除 Refresh Token 並清除 refresh_token cookie")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "登出成功")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken,
            HttpServletRequest httpRequest) {

        String authHeader = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
        String accessToken = authHeader != null && authHeader.startsWith(BEARER_PREFIX)
                ? authHeader.substring(BEARER_PREFIX.length())
                : null;
        authService.logout(accessToken, refreshToken == null || refreshToken.isBlank() ? null : refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear().toString())
                .body(ApiResponse.success("已成功登出", null));
    }
}
