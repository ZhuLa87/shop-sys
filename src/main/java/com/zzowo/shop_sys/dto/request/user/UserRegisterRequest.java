package com.zzowo.shop_sys.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterRequest {

    @NotBlank(message = "Email 不可為空")
    @Email(message = "Email 格式不正確")
    private String email;

    @NotBlank(message = "密碼不可為空")
    @Size(min = 8, message = "密碼長度至少需要 8 個字元")
    private String password;

    @NotBlank(message = "姓名不可為空")
    private String name;

    private String phone; // 電話可選，若有特定格式可加 @Pattern
}