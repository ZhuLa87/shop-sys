package com.zzowo.shop_sys.dto.request.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "Token 刷新請求")
@Data
public class TokenRefreshRequest {

    @Schema(description = "Refresh Token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    @NotBlank(message = "refreshToken 不可為空")
    private String refreshToken;
}
