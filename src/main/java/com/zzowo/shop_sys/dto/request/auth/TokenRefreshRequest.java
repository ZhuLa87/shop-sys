package com.zzowo.shop_sys.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenRefreshRequest {
    @NotBlank(message = "refreshToken 不可為空")
    private String refreshToken;
}
