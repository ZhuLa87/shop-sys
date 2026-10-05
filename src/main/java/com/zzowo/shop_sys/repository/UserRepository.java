package com.zzowo.shop_sys.repository;

import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.Role;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // 依目前登入者的 Email (JWT subject) 取得使用者, 各 service 共用同一個錯誤訊息
    default User getByEmailOrThrow(String email) {
        return findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("使用者不存在"));
    }

    boolean existsByEmail(String email);

    // 計算除了指定使用者以外,還有幾個啟用中的該角色帳號
    // 用於確保系統永遠保留至少一位可登入的最高管理員 (@SQLRestriction 已排除軟刪除者)
    long countByRoleAndEnabledIsTrueAndIdNot(Role role, Long excludedUserId);
}