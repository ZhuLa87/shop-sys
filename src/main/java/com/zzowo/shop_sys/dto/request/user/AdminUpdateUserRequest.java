package com.zzowo.shop_sys.dto.request.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理員更新使用者請求（所有欄位皆為選填，僅更新有值的欄位）")
@Data
public class AdminUpdateUserRequest {

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

    @Schema(description = "角色：CUSTOMER / PRODUCT_MANAGER / ORDER_MANAGER / CUSTOMER_SERVICE / MARKETING / FINANCE / SUPER_ADMIN", example = "CUSTOMER")
    private String role;

    @Schema(description = "帳號是否啟用", example = "true")
    private Boolean enabled;
}
