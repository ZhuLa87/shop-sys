package com.zzowo.shop_sys.dto.request.user;

import io.micrometer.common.lang.Nullable;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegisterRequest {
    @NotBlank
    private String email;
    @NotBlank
    private String password;
    @NotBlank
    private String name;
    @Nullable
    private String phone;
}
