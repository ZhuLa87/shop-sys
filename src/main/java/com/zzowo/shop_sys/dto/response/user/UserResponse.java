package com.zzowo.shop_sys.dto.response.user;

import lombok.Data;

@Data
public class UserResponse {
    private String email;
    private String name;
    private String role;
    private String phone;
    // 使用 Long (Timestamp) 儲存時間資訊
    private Long createdAt;
    private Long lastLoginAt;
    private String lastPasswordChangeAt;
}