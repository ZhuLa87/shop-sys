package com.zzowo.shop_sys.dto.response.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Token 刷新成功回應")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshResponse {

    @Schema(description = "新的 Access Token (JWT) ", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "新的 Refresh Token (Token Rotation - 舊 Token 已失效) ", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "Token 類型", example = "Bearer")
    private String tokenType;

    @Schema(description = "Access Token 有效期 (秒) ", example = "1800")
    private long expiresIn;
}
