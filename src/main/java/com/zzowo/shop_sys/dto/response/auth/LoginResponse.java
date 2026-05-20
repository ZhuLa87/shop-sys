package com.zzowo.shop_sys.dto.response.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "登入成功回應")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    @Schema(description = "Access Token (JWT) ,請放入後續請求的 Authorization: Bearer 標頭", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "Refresh Token,用於換發新的 Access Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "Token 類型", example = "Bearer")
    private String tokenType;

    @Schema(description = "Access Token 有效期 (秒) ", example = "1800")
    private long expiresIn;
}
