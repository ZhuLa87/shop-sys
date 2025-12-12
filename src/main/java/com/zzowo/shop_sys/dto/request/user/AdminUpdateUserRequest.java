package com.zzowo.shop_sys.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUpdateUserRequest {

    @Email(message = "Email 格式不正確")
    private String email;

    @Size(min = 8, message = "新密碼長度至少需要 8 個字元")
    private String password;

    private String name;

    private String phone;

    // 針對角色，建議未來可用 Enum 驗證，目前先維持字串
    private String role;

    private Boolean enabled;
}