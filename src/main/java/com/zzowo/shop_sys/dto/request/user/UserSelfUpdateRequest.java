package com.zzowo.shop_sys.dto.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "使用者自我更新請求（所有欄位皆為選填，僅更新有值的欄位）")
@Data
public class UserSelfUpdateRequest {

    @Schema(description = "電子郵件", example = "user@example.com")
    @Email(message = "Email 格式不正確")
    private String email;

    @Schema(description = "新密碼（最少 8 個字元），留空則不修改", example = "newPassword123")
    @Size(min = 8, message = "新密碼長度至少需要 8 個字元")
    private String password;

    @Schema(description = "姓名", example = "王小明")
    private String name;

    @Schema(description = "電話", example = "0912345678")
    private String phone;
}
