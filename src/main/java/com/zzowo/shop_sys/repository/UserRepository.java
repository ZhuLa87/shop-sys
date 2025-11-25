package com.zzowo.shop_sys.repository;

import com.zzowo.shop_sys.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // 檢查 Email 是否已存在 (註冊時用)
    boolean existsByEmail(String email);
}