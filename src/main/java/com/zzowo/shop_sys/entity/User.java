package com.zzowo.shop_sys.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.zzowo.shop_sys.enums.Role;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@SQLRestriction("deleted_at IS NULL")
@Data
@Entity
@Table(name = "users")
public class User implements UserDetails {

    // 主鍵 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 使用者 Email,需唯一且不可為 null
    @Column(nullable = false, unique = true)
    private String email;

    // 密碼 Hash
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

    // 帳號是否未過期
    @Column(name = "is_account_non_expired", nullable = false)
    private Boolean accountNonExpired = true;

    // 帳號是否未被鎖
    @Column(name = "is_account_non_locked", nullable = false)
    private Boolean accountNonLocked = true;

    // 頭像圖片 URL
    @Column(name = "avatar_url")
    private String avatarUrl;

    // 密碼是否未過期
    @Column(name = "is_credentials_non_expired", nullable = false)
    private Boolean credentialsNonExpired = true;

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

    // 軟刪除使用者
    public void delete() {
        deletedAt = LocalDateTime.now();
        enabled = false;
    }

    // 傳回使用者擁有的權限 (Authorities) 
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring Security 規定角色格式要是 "ROLE_XXX"
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    // 回傳使用者密碼
    @Override
    public String getPassword() {
        return passwordHash;
    }

    // 回傳使用者帳號 (email 即 username) 
    @Override
    public String getUsername() {
        return email;
    }

    // 帳號是否未過期 (true = 沒有過期) 
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    // 帳號是否未被鎖 (true = 沒被鎖) 
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    // 密碼是否未過期 (true = 沒過期)
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    // 帳號是否啟用 (true = 啟用) . UserDetails 預設 (default method) 恆回傳 true,必須覆寫才會讀到 enabled 欄位
    @Override
    public boolean isEnabled() {
        return enabled;
    }

}
