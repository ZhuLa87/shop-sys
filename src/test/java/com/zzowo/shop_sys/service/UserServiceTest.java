package com.zzowo.shop_sys.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.zzowo.shop_sys.dto.request.user.AdminUpdateUserRequest;
import com.zzowo.shop_sys.dto.request.user.UserLoginRequest;
import com.zzowo.shop_sys.dto.request.user.UserRegisterRequest;
import com.zzowo.shop_sys.dto.request.user.UserSelfUpdateRequest;
import com.zzowo.shop_sys.dto.response.auth.LoginResponse;
import com.zzowo.shop_sys.dto.response.user.RegisterResponse;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.Role;
import com.zzowo.shop_sys.exception.BusinessException;
import com.zzowo.shop_sys.exception.ResourceNotFoundException;
import com.zzowo.shop_sys.mapper.UserMapper;
import com.zzowo.shop_sys.repository.UserRepository;
import com.zzowo.shop_sys.util.JwtUtil;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtUtil jwtUtil;
    @Mock RefreshTokenService refreshTokenService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserMapper userMapper;
    @InjectMocks UserService userService;

    @Test
    void login_validCredentials_returnsTokens() {
        User user = buildUser(true, true);
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(jwtUtil.generateToken(user)).thenReturn("access-token");
        when(jwtUtil.getAccessExpirationSeconds()).thenReturn(1800L);
        when(refreshTokenService.create(1L)).thenReturn("refresh-token");

        LoginResponse response = userService.login(loginRequest());

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(user.getLastLoginAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void login_wrongPassword_throws401() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> userService.login(loginRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("帳號或密碼錯誤")
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(refreshTokenService, never()).create(any());
    }

    @Test
    void login_disabledAccount_throws423() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));

        assertThatThrownBy(() -> userService.login(loginRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.LOCKED);
    }

    @Test
    void login_lockedAccount_throws423() {
        when(authenticationManager.authenticate(any())).thenThrow(new LockedException("locked"));

        assertThatThrownBy(() -> userService.login(loginRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.LOCKED);
    }

    @Test
    void assertAccountActive_disabledUser_throws423() {
        assertThatThrownBy(() -> userService.assertAccountActive(buildUser(false, true)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.LOCKED);
    }

    @Test
    void assertAccountActive_lockedUser_throws423() {
        assertThatThrownBy(() -> userService.assertAccountActive(buildUser(true, false)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.LOCKED);
    }

    @Test
    void assertAccountActive_activeUser_passes() {
        userService.assertAccountActive(buildUser(true, true));
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    void register_success_encodesPassword_andDefaultsToCustomerRole() {
        UserRegisterRequest request = registerRequest();
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(new User());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-hash");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toRegisterResponse(any())).thenReturn(new RegisterResponse());

        userService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("encoded-hash");
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(saved.getLastPasswordChangeAt()).isNotNull();
    }

    @Test
    void register_duplicateEmail_throws_andSavesNothing() {
        when(userRepository.existsByEmail("new@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(registerRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("帳號已被註冊");

        verify(userRepository, never()).save(any());
    }

    // ── updateMyInfo ─────────────────────────────────────────────────────────

    @Test
    void updateMyInfo_blankFields_keepExistingValues() {
        User user = buildUser(true, true);
        user.setName("原本的名字");
        when(userRepository.findByEmail("user@test.com")).thenReturn(java.util.Optional.of(user));

        userService.updateMyInfo("user@test.com", new UserSelfUpdateRequest()); // 全部欄位留空

        assertThat(user.getName()).isEqualTo("原本的名字");
        assertThat(user.getEmail()).isEqualTo("user@test.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateMyInfo_newPassword_isEncoded() {
        User user = buildUser(true, true);
        when(userRepository.findByEmail("user@test.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-hash");

        UserSelfUpdateRequest request = new UserSelfUpdateRequest();
        request.setPassword("newPassword123");
        userService.updateMyInfo("user@test.com", request);

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    }

    @Test
    void updateMyInfo_emailAlreadyTakenByAnotherUser_throws() {
        User user = buildUser(true, true);
        when(userRepository.findByEmail("user@test.com")).thenReturn(java.util.Optional.of(user));
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        UserSelfUpdateRequest request = new UserSelfUpdateRequest();
        request.setEmail("taken@test.com");

        assertThatThrownBy(() -> userService.updateMyInfo("user@test.com", request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已被註冊");

        assertThat(user.getEmail()).isEqualTo("user@test.com"); // 未被改動
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMyInfo_sameEmailAsOwn_doesNotTriggerDuplicateCheck() {
        User user = buildUser(true, true);
        when(userRepository.findByEmail("user@test.com")).thenReturn(java.util.Optional.of(user));

        UserSelfUpdateRequest request = new UserSelfUpdateRequest();
        request.setEmail("user@test.com"); // 與自己現有的 email 相同

        userService.updateMyInfo("user@test.com", request);

        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository).save(user);
    }

    @Test
    void updateMyInfo_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> userService.updateMyInfo("ghost@test.com", new UserSelfUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateUserByAdmin ────────────────────────────────────────────────────

    @Test
    void updateUserByAdmin_updatesRoleAndEnabledFlag() {
        User user = buildUser(true, true);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));

        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setRole("PRODUCT_MANAGER");
        request.setEnabled(false);
        userService.updateUserByAdmin(1L, request);

        assertThat(user.getRole()).isEqualTo(Role.PRODUCT_MANAGER);
        assertThat(user.getEnabled()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void updateUserByAdmin_emailAlreadyTaken_throws() {
        User user = buildUser(true, true);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(user));
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setEmail("taken@test.com");

        assertThatThrownBy(() -> userService.updateUserByAdmin(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已被註冊");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserByAdmin_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> userService.updateUserByAdmin(99L, new AdminUpdateUserRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private UserRegisterRequest registerRequest() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setEmail("new@test.com");
        request.setPassword("password123");
        request.setName("王小明");
        return request;
    }

    private User buildUser(boolean enabled, boolean accountNonLocked) {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.com");
        user.setPasswordHash("hashed");
        user.setRole(Role.CUSTOMER);
        user.setEnabled(enabled);
        user.setAccountNonLocked(accountNonLocked);
        return user;
    }

    private UserLoginRequest loginRequest() {
        UserLoginRequest request = new UserLoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("password123");
        return request;
    }
}
