package com.zzowo.shop_sys.dto.request.user;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UserSelfUpdateRequest {

    @Email(message = "Invalid email format")
    private String email;

    private String password;

    private String name;

    private String phone;
}
