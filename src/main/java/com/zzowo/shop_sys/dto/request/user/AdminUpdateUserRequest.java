package com.zzowo.shop_sys.dto.request.user;

import lombok.Data;

@Data
public class AdminUpdateUserRequest {

    private String email;

    private String password;

    private String name;

    private String phone;

    // Admin only
    private String role;

    private Boolean enabled;
}
