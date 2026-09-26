package com.zzowo.shop_sys.service;

import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.zzowo.shop_sys.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.zzowo.shop_sys.dto.request.user.AdminUpdateUserRequest;
import com.zzowo.shop_sys.dto.request.user.UserRegisterRequest;
import com.zzowo.shop_sys.dto.request.user.UserSelfUpdateRequest;
import com.zzowo.shop_sys.dto.response.user.RegisterResponse;
import com.zzowo.shop_sys.dto.response.user.UserResponse;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.Role;
import com.zzowo.shop_sys.exception.BusinessException;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final UserMapper userMapper;

    private final MinioService minioService;

    // 取得目前登入者的個人資料
    public UserResponse getUserProfile(String email) {
        User user = userRepository.getByEmailOrThrow(email);
        return userMapper.toUserResponse(user);
    }

    @Transactional
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

    // 一般使用者更新自己的資料
    @Transactional
    public void updateMyInfo(String currentEmail, UserSelfUpdateRequest request) {
        User user = userRepository.getByEmailOrThrow(currentEmail);

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
        if (StringUtils.hasText(request.getAvatarUrl())) {
            // 只接受透過 /upload/presign 上傳到自己頭像目錄的檔案
            if (!minioService.isPublicUrlUnder(request.getAvatarUrl(), "avatars/" + user.getId() + "/")) {
                throw new BusinessException("頭像 URL 不合法,請重新上傳");
            }
            user.setAvatarUrl(request.getAvatarUrl());
        }

        userRepository.save(user);
    }

    // 超級管理員更新任何人的資料
    @Transactional
    public void updateUserByAdmin(String operatorEmail, Long userId, AdminUpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到使用者 ID: " + userId));

        // 停用或降級前先擋下會讓系統再也沒有管理員可用的操作
        // 這個端點本身需要 SUPER_ADMIN,一旦最後一位被鎖在門外就只能進資料庫手動修
        assertNotLockingOutAdmins(operatorEmail, user, request);

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
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getEnabled() != null) user.setEnabled(request.getEnabled());

        userRepository.save(user);
    }

    // 擋下會把管理員鎖在系統外的操作
    private void assertNotLockingOutAdmins(String operatorEmail, User target, AdminUpdateUserRequest request) {
        User operator = userRepository.getByEmailOrThrow(operatorEmail);
        boolean isSelf = operator.getId().equals(target.getId());

        // 停用帳號
        if (Boolean.FALSE.equals(request.getEnabled()) && target.isEnabled()) {
            if (isSelf) {
                throw new BusinessException("不可停用自己的帳號");
            }
            assertNotLastActiveSuperAdmin(target, "停用");
        }

        // 變更角色
        if (request.getRole() != null) {
            Role newRole = request.getRole();
            if (newRole == target.getRole()) {
                return;
            }
            if (isSelf && target.getRole() == Role.SUPER_ADMIN) {
                throw new BusinessException("不可變更自己的角色,請由另一位最高管理員操作");
            }
            if (target.getRole() == Role.SUPER_ADMIN) {
                assertNotLastActiveSuperAdmin(target, "變更角色");
            }
        }
    }

    // 系統必須永遠保留至少一位啟用中的最高管理員
    private void assertNotLastActiveSuperAdmin(User target, String action) {
        if (target.getRole() != Role.SUPER_ADMIN || !target.isEnabled()) {
            return;
        }
        if (userRepository.countByRoleAndEnabledIsTrueAndIdNot(Role.SUPER_ADMIN, target.getId()) == 0) {
            throw new BusinessException("無法" + action + ":系統必須保留至少一位啟用中的最高管理員");
        }
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