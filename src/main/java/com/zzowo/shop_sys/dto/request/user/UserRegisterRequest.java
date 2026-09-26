package com.zzowo.shop_sys.dto.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "使用者註冊請求")
@Data
public class UserRegisterRequest {

    @Schema(description = "電子郵件 (系統唯一) ", example = "user@example.com")
    @NotBlank(message = "Email 不可為空")
    @Email(message = "Email 格式不正確")
    @Size(max = 255, message = "長度不可超過 255 個字元")
    private String email;

    @Schema(description = "密碼 (最少 8 個字元) ", example = "password123")
    @NotBlank(message = "密碼不可為空")
    @Size(min = 8, max = 128, message = "密碼長度需為 8 到 128 個字元")
    private String password;

    @Schema(description = "姓名", example = "王小明")
    @NotBlank(message = "姓名不可為空")
    @Size(max = 255, message = "長度不可超過 255 個字元")
    private String name;

    @Schema(description = "電話 (選填) ", example = "0912345678")
    @Size(max = 255, message = "長度不可超過 255 個字元")
    private String phone;
}
