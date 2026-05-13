package com.zzowo.shop_sys.dto.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "使用者登入請求")
@Data
public class UserLoginRequest {

    @Schema(description = "電子郵件", example = "user@example.com")
    @NotBlank(message = "Email 不可為空")
    @Email(message = "Email 格式不正確")
    private String email;

    @Schema(description = "密碼", example = "password123")
    @NotBlank(message = "密碼不可為空")
    private String password;
}
