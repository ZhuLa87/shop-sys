package com.zzowo.shop_sys.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

import com.zzowo.shop_sys.enums.Role;

@Data
@Entity
@Table(name = "users")
public class User {

    // 主鍵 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    // 使用者 Email，需唯一且不可為 null
    @Column(nullable = false, unique = true)
    private String email;

    // 密碼的 Hash 值
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // 角色
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CUSTOMER;

    // 名字
    @Column(name = "name")
    private String name;

    // 電話
    @Column(name = "phone")
    private String phone;

    // 帳戶是否啟用
    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled = true;

    // 建立時間
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 更新時間
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 刪除時間 (軟刪除)
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 最後登入時間
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // 最後更改密碼時間
    @Column(name = "last_password_change_at")
    private LocalDateTime lastPasswordChangeAt;

    @PrePersist
    protected void onCreate() {
        // 在資料新增時自動設定建立與更新時間
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        // 在資料更新時自動刷新 updatedAt
        updatedAt = LocalDateTime.now();
    }
}
