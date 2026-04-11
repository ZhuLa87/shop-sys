package com.zzowo.shop_sys.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserSelfUpdateRequest {

    @Email(message = "Email 格式不正確")
    private String email;

    // 若使用者想改密碼，長度需符合規定；若留空則代表不修改
    @Size(min = 8, message = "新密碼長度至少需要 8 個字元")
    private String password;

    private String name;

    private String phone;
}