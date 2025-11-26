package com.zzowo.shop_sys.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.zzowo.shop_sys.dto.request.user.UserLoginRequest;
import com.zzowo.shop_sys.dto.request.user.UserRegisterRequest;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.Role;
import com.zzowo.shop_sys.repository.UserRepository;
import com.zzowo.shop_sys.util.JwtUtil;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public User register(UserRegisterRequest request) {

        // 1. 檢查 Email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // 2. 轉換 DTO 成 Entity
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword())); // 加密
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setRole(Role.CUSTOMER);

        // 3. 存入資料庫
        return userRepository.save(user);
    }

    public String login(UserLoginRequest request) {
        // 1. 根據 Email 尋找使用者
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("帳號不存在"));

        // 2. 驗證密碼 (拿明碼跟資料庫的亂碼比對)
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("帳號或密碼錯誤");
        }

        // 3. 驗鄭成功，生成 JWT
        return jwtUtil.generateToken(user);
    }
}
