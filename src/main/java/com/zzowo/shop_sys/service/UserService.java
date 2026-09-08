package com.zzowo.shop_sys.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.zzowo.shop_sys.mapper.UserMapper; // Import Mapper
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import Transactional
import org.springframework.util.StringUtils;

import com.zzowo.shop_sys.dto.request.user.AdminUpdateUserRequest;
import com.zzowo.shop_sys.dto.request.user.UserLoginRequest;
import com.zzowo.shop_sys.dto.request.user.UserRegisterRequest;
import com.zzowo.shop_sys.dto.request.user.UserSelfUpdateRequest;
import com.zzowo.shop_sys.dto.response.auth.LoginResponse;
import com.zzowo.shop_sys.dto.response.user.RegisterResponse;
import com.zzowo.shop_sys.dto.response.user.UserResponse;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.Role;
import com.zzowo.shop_sys.exception.BusinessException;
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

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 取得特定 email 的使用者詳細資料
     * @param email
     * @return
     */
    public UserResponse getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("找不到使用者"));
        return userMapper.toUserResponse(user);
    }

    @Transactional // 加入事務管理
    public RegisterResponse register(UserRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("帳號已被註冊");
        }

        // 使用 Mapper 轉換基本資料
        User user = userMapper.toEntity(request);

        // 處理業務邏輯 (加密,預設值)
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CUSTOMER);
        user.setLastPasswordChangeAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        return userMapper.toRegisterResponse(savedUser);
    }

    @Transactional
    public LoginResponse login(UserLoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new BusinessException("帳號或密碼錯誤", HttpStatus.UNAUTHORIZED);
        } catch (DisabledException | LockedException e) {
            throw new BusinessException("帳號已被停用或鎖定,請聯繫客服", HttpStatus.LOCKED);
        }

        User user = (User) authentication.getPrincipal();
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = refreshTokenService.create(user.getId());

        return new LoginResponse(accessToken, refreshToken, "Bearer", jwtUtil.getAccessExpirationSeconds());
    }

    // 供 /auth/refresh 換發 token 前檢查帳號狀態,避免帳號被停用/鎖定後仍能無限期換發 access token
    public void assertAccountActive(User user) {
        if (!user.isEnabled() || !user.isAccountNonLocked()) {
            throw new BusinessException("帳號已被停用或鎖定,請聯繫客服", HttpStatus.LOCKED);
        }
    }

    @Transactional // 加入事務管理
    // 一般使用者更新自己的資料
    public void updateMyInfo(String currentEmail, UserSelfUpdateRequest request) {
        // 找出是誰在操作
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("使用者不存在"));

        // 如果要改 Email,需檢查新 Email 是否已被其他人使用
        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("該 Email 已被註冊");
            }
            user.setEmail(request.getEmail());
        }

        // 如果有傳密碼,就重新加密設定
        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        // 更新其他基本資料 (如果有傳值才更新)
        if (StringUtils.hasText(request.getName())) user.setName(request.getName());
        if (StringUtils.hasText(request.getPhone())) user.setPhone(request.getPhone());
        if (StringUtils.hasText(request.getAvatarUrl())) user.setAvatarUrl(request.getAvatarUrl());

        userRepository.save(user);
    }

    @Transactional // 加入事務管理
    // 超級管理員更新任何人的資料
    public void updateUserByAdmin(Long userId, AdminUpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到使用者 ID: " + userId));

        // 管理員修改 Email 也要檢查重複
        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("該 Email 已被註冊");
            }
            user.setEmail(request.getEmail());
        }

        // 管理員重設密碼
        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        // 更新基本資料
        if (StringUtils.hasText(request.getName())) user.setName(request.getName());
        if (StringUtils.hasText(request.getPhone()))
            user.setPhone(request.getPhone());
        // 更新權限與狀態 (管理員特權)
        if (request.getRole() != null) user.setRole(Role.valueOf(request.getRole()));
        if (request.getEnabled() != null) user.setEnabled(request.getEnabled());

        userRepository.save(user);
    }

    // 超級管理員取得特定使用者資訊
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("找不到使用者 ID: " + id));
        return userMapper.toUserResponse(user);
    }

    // 超級管理員取得所有使用者資訊
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
    }
}