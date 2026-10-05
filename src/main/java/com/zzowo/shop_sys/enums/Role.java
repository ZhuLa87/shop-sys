package com.zzowo.shop_sys.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "使用者角色")
public enum Role {
    CUSTOMER,
    FINANCE,
    MARKETING,
    CUSTOMER_SERVICE,
    ORDER_MANAGER,
    PRODUCT_MANAGER,
    SUPER_ADMIN;

    // Spring Security 的角色 authority 必須以 "ROLE_" 開頭
    public static final String AUTHORITY_PREFIX = "ROLE_";

    public String getAuthority() {
        return AUTHORITY_PREFIX + name();
    }
}