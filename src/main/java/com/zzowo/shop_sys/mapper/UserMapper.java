package com.zzowo.shop_sys.mapper;

import com.zzowo.shop_sys.dto.request.user.UserRegisterRequest;
import com.zzowo.shop_sys.dto.response.user.RegisterResponse;
import com.zzowo.shop_sys.dto.response.user.UserResponse;
import com.zzowo.shop_sys.entity.User;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        if (user == null)
            return null;

        UserResponse response = new UserResponse();
        response.setEmail(user.getEmail());
        response.setName(user.getName());
        response.setRole(user.getRole().name());
        response.setPhone(user.getPhone());

        if (user.getCreatedAt() != null) {
            response.setCreatedAt(user.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        if (user.getLastLoginAt() != null) {
            response.setLastLoginAt(user.getLastLoginAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        if (user.getLastPasswordChangeAt() != null) {
            response.setLastPasswordChangeAt(user.getLastPasswordChangeAt().toString());
        }

        return response;
    }

    public RegisterResponse toRegisterResponse(User user) {
        if (user == null) return null;

        RegisterResponse response = new RegisterResponse();
        response.setEmail(user.getEmail());
        response.setName(user.getName());
        response.setRole(user.getRole().name());
        response.setPhone(user.getPhone());

        if (user.getCreatedAt() != null) {
            response.setCreatedAt(user.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }

        return response;
    }

    public User toEntity(UserRegisterRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        // 密碼與角色涉及業務邏輯，保留在 Service 處理
        return user;
    }
}