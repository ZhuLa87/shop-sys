package com.zzowo.shop_sys.dto.response.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "使用者資訊回應")
@Data
public class UserResponse {

    @Schema(description = "電子郵件", example = "user@example.com")
    private String email;

    @Schema(description = "姓名", example = "王小明")
    private String name;

    @Schema(description = "角色", example = "CUSTOMER")
    private String role;

    @Schema(description = "電話", example = "0912345678")
    private String phone;

    @Schema(description = "帳號建立時間 (Unix 毫秒時間戳) ", example = "1715000000000")
    private Long createdAt;

    @Schema(description = "最後登入時間 (Unix 毫秒時間戳) ", example = "1715600000000")
    private Long lastLoginAt;

    @Schema(description = "最後修改密碼時間", example = "2024-01-15T10:30:00")
    private String lastPasswordChangeAt;
}
