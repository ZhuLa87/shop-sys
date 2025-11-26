package com.zzowo.shop_sys.dto.request.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegisterRequest {
    private String email;
    private String password_hash;
    private String name;
    private String phone;
}
