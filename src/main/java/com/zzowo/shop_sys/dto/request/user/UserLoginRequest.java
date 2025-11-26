package com.zzowo.shop_sys.dto.request.user;

import lombok.Data;

@Data
public class UserLoginRequest {
    private String email;
    private String password;
}