package com.zzowo.shop_sys.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.zzowo.shop_sys.dto.request.user.UserRegisterRequest;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.Role;
import com.zzowo.shop_sys.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // 這裡之後要注入 PasswordEncoder
    @Autowired
    private PasswordEncoder passwordEncoder;

    public User register(UserRegisterRequest request) {

        // 1. 檢查 Email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // 2. 轉換 DTO 成 Entity
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));  // 加密
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setRole(Role.CUSTOMER);

        // 3. 存入資料庫
        return userRepository.save(user);
    }
}
