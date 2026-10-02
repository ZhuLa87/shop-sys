package com.zzowo.shop_sys.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.zzowo.shop_sys.dto.request.user.AdminUpdateUserRequest;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.Role;
import com.zzowo.shop_sys.repository.UserRepository;

// 撤銷 token 必須在帳號異動 commit 之後才執行 (理由見 AuthService.onUserSessionsRevoked),
// rollback 時不能執行. @TransactionalEventListener 的行為只有在真的 Spring transaction 裡才驗證得了
@SpringBootTest
class UserSessionsRevokedAfterCommitTest {

    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired TransactionTemplate transactionTemplate;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbcTemplate;

    @MockitoBean TokenBlacklistService tokenBlacklistService;
    @MockitoBean RefreshTokenService refreshTokenService;

    @Test
    void disableUser_revokesTokens_onlyAfterCommit() {
        User operator = saveUser(Role.SUPER_ADMIN);
        User target = saveUser(Role.PRODUCT_MANAGER);

        // 撤銷執行的當下, 用另一條連線 (REQUIRES_NEW) 讀到的必須已經是停用: 代表變更已經 commit
        AtomicReference<Boolean> enabledSeenByOthers = new AtomicReference<>();
        doAnswer(inv -> {
            enabledSeenByOthers.set(isEnabledInNewTransaction(target.getId()));
            return null;
        }).when(tokenBlacklistService).revokeAllForUser(target.getId());

        transactionTemplate.executeWithoutResult(status -> {
            userService.updateUserByAdmin(operator.getEmail(), target.getId(), disable());

            // 還在 transaction 裡: 尚未撤銷
            verify(tokenBlacklistService, never()).revokeAllForUser(any());
            verify(refreshTokenService, never()).revokeForUser(any());
        });

        verify(tokenBlacklistService).revokeAllForUser(target.getId());
        verify(refreshTokenService).revokeForUser(target.getId());
        assertThat(enabledSeenByOthers.get()).isFalse();
    }

    @Test
    void disableUser_rolledBack_doesNotRevokeTokens() {
        User operator = saveUser(Role.SUPER_ADMIN);
        User target = saveUser(Role.PRODUCT_MANAGER);

        transactionTemplate.executeWithoutResult(status -> {
            userService.updateUserByAdmin(operator.getEmail(), target.getId(), disable());
            status.setRollbackOnly();
        });

        verify(tokenBlacklistService, never()).revokeAllForUser(any());
        verify(refreshTokenService, never()).revokeForUser(any());
    }

    private boolean isEnabledInNewTransaction(Long userId) {
        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return requiresNew.execute(status -> jdbcTemplate.queryForObject(
                "SELECT is_enabled FROM users WHERE id = ?", Boolean.class, userId));
    }

    private AdminUpdateUserRequest disable() {
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setEnabled(false);
        return request;
    }

    private User saveUser(Role role) {
        User user = new User();
        user.setEmail(role.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        user.setPasswordHash("not-used");
        user.setRole(role);
        return userRepository.save(user);
    }
}
