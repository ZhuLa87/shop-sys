package com.zzowo.shop_sys.util;

import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "testSecretKeyForShopSYSThatIsLongEnoughForHS512Algorithm";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 1800000L); // 30 分鐘
    }

    // ── generateToken ────────────────────────────────────────────────────────

    @Test
    void generateToken_containsJtiClaim() {
        String token = jwtUtil.generateToken(mockUser());

        assertThat(jwtUtil.getJtiFromToken(token)).isNotBlank();
    }

    @Test
    void generateToken_eachTokenHasUniqueJti() {
        User user = mockUser();
        String token1 = jwtUtil.generateToken(user);
        String token2 = jwtUtil.generateToken(user);

        assertThat(jwtUtil.getJtiFromToken(token1))
                .isNotEqualTo(jwtUtil.getJtiFromToken(token2));
    }

    @Test
    void generateToken_containsUserIdAndRoleClaims() {
        String token = jwtUtil.generateToken(mockUser());

        assertThat(jwtUtil.getUserIdFromToken(token)).isEqualTo(1L);
        assertThat(jwtUtil.getRoleFromToken(token)).isEqualTo(Role.CUSTOMER.name());
        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo("test@example.com");
    }

    // ── validateToken ────────────────────────────────────────────────────────

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtil.generateToken(mockUser());

        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_expiredToken_returnsFalse() {
        // 將 expiration 設為負數，產生一個「發行時已過期」的 token
        ReflectionTestUtils.setField(jwtUtil, "expiration", -10000L);
        String token = jwtUtil.generateToken(mockUser());

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken(mockUser()) + "tampered";

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    // ── getJtiFromToken ──────────────────────────────────────────────────────

    @Test
    void getJtiFromToken_expiredToken_stillReturnsJti() {
        // 過期的 token 仍可取出 jti（供黑名單 TTL 計算使用）
        ReflectionTestUtils.setField(jwtUtil, "expiration", -10000L);
        String token = jwtUtil.generateToken(mockUser());

        assertThat(jwtUtil.getJtiFromToken(token)).isNotBlank();
    }

    @Test
    void getJtiFromToken_invalidToken_returnsNull() {
        assertThat(jwtUtil.getJtiFromToken("not.a.jwt")).isNull();
    }

    // ── getExpirationDateFromToken ───────────────────────────────────────────

    @Test
    void getExpirationDateFromToken_expiredToken_returnsExpiration() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -10000L);
        String token = jwtUtil.generateToken(mockUser());

        Date expiry = jwtUtil.getExpirationDateFromToken(token);

        assertThat(expiry).isNotNull().isBefore(new Date());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private User mockUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPasswordHash("hash");
        user.setRole(Role.CUSTOMER);
        return user;
    }
}
