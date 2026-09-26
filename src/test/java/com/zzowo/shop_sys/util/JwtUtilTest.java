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

        var claims = jwtUtil.parseClaims(token).orElseThrow();
        assertThat(((Number) claims.get("userId")).longValue()).isEqualTo(1L);
        assertThat(claims.get(JwtUtil.CLAIM_ROLE, String.class)).isEqualTo(Role.CUSTOMER.name());
        assertThat(claims.getSubject()).isEqualTo("test@example.com");
    }

    @Test
    void parseClaims_tamperedToken_returnsEmpty() {
        String token = jwtUtil.generateToken(mockUser()) + "tampered";

        assertThat(jwtUtil.parseClaims(token)).isEmpty();
    }

    // ── getJtiFromToken ──────────────────────────────────────────────────────

    @Test
    void getJtiFromToken_expiredToken_stillReturnsJti() {
        // 過期的 token 仍可取出 jti (供黑名單 TTL 計算使用) 
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

    // ── parseClaims ──────────────────────────────────────────────────────────

    @Test
    void parseClaims_validToken_returnsSubjectRoleAndJti() {
        String token = jwtUtil.generateToken(mockUser());

        var claims = jwtUtil.parseClaims(token).orElseThrow();

        assertThat(claims.getSubject()).isEqualTo(mockUser().getEmail());
        assertThat(claims.get(JwtUtil.CLAIM_ROLE, String.class)).isEqualTo(mockUser().getRole().name());
        assertThat(claims.getId()).isNotBlank();
    }

    @Test
    void parseClaims_expiredToken_returnsEmpty() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -10000L);
        String token = jwtUtil.generateToken(mockUser());

        assertThat(jwtUtil.parseClaims(token)).isEmpty();
    }

    @Test
    void parseClaims_garbageOrBlank_returnsEmpty() {
        assertThat(jwtUtil.parseClaims("not-a-jwt")).isEmpty();
        assertThat(jwtUtil.parseClaims("")).isEmpty();
        assertThat(jwtUtil.parseClaims(null)).isEmpty();
    }

    private User mockUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPasswordHash("hash");
        user.setRole(Role.CUSTOMER);
        return user;
    }
}
