package com.zzowo.shop_sys.service;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.zzowo.shop_sys.dto.request.user.AdminUpdateUserRequest;
import com.zzowo.shop_sys.dto.request.user.UserLoginRequest;
import com.zzowo.shop_sys.dto.request.user.UserRegisterRequest;
import com.zzowo.shop_sys.dto.request.user.UserSelfUpdateRequest;
import com.zzowo.shop_sys.dto.response.user.UserResponse;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.Role;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
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

    /**
     * 取得特定 email 的使用者詳細資料
     * @param email
     * @return
     */
    public UserResponse getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("找不到使用者"));

        UserResponse response = new UserResponse();
        response.setEmail(user.getEmail());
        response.setName(user.getName());
        response.setRole(user.getRole().name());
        response.setPhone(user.getPhone());
        response.setCreatedAt(user.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        response.setLastLoginAt(user.getLastLoginAt() != null ? user.getLastLoginAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null);
        response.setLastPasswordChangeAt(user.getLastPasswordChangeAt() != null ? user.getLastPasswordChangeAt().toString() : null);

        return response;
    }

    public User register(UserRegisterRequest request) {

        // 檢查 Email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("帳號已被註冊");
        }

        // 轉換 DTO 成 Entity
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword())); // 加密
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setRole(Role.CUSTOMER);
        user.setLastPasswordChangeAt(LocalDateTime.now());

        // 存入資料庫
        return userRepository.save(user);
    }

    public String login(UserLoginRequest request) {
        // 根據 Email 尋找使用者
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("帳號不存在"));

        // 驗證密碼 (拿明碼跟資料庫的亂碼比對)
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("帳號或密碼錯誤");
        }

        // 更新最後登入時間
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 驗證成功，生成 JWT
        return jwtUtil.generateToken(user);
    }

    // 一般使用者更新自己的資料
    public void updateMyInfo(String currentEmail, UserSelfUpdateRequest request) {
        // 先找出是誰在操作
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("使用者不存在"));

        // 如果要改 Email，需檢查新 Email 是否已被其他人使用
        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("該 Email 已被註冊");
            }
            user.setEmail(request.getEmail());
        }

        // 如果有傳密碼，就重新加密設定
        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        // 更新其他基本資料 (如果有傳值才更新)
        if (StringUtils.hasText(request.getName())) user.setName(request.getName());
        if (StringUtils.hasText(request.getPhone())) user.setPhone(request.getPhone());

        userRepository.save(user);
    }

    // 超級管理員更新任何人的資料
    public void updateUserByAdmin(Long userId, AdminUpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到使用者 ID: " + userId));

        // 管理員修改 Email 也要檢查重複
        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("該 Email 已被註冊");
            }
            user.setEmail(request.getEmail());
        }

        // 管理員重設密碼
        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        // 更新基本資料
        if (StringUtils.hasText(request.getName()))
            user.setName(request.getName());
        if (StringUtils.hasText(request.getPhone()))
            user.setPhone(request.getPhone());

        // 更新權限與狀態 (管理員特權)
        if (request.getRole() != null)
            user.setRole(Role.valueOf(request.getRole()));
        if (request.getEnabled() != null)
            user.setEnabled(request.getEnabled());

        userRepository.save(user);
    }

    public UserResponse getUserById(Long id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUserById'");
    }

    public Object getAllUsers() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllUsers'");
    }
}
