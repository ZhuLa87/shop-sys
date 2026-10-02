package com.zzowo.shop_sys.service;

import com.zzowo.shop_sys.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    StringRedisTemplate redisTemplate;

    @Mock
    ValueOperations<String, String> valueOps;

    @Mock
    JwtUtil jwtUtil;

    @InjectMocks
    TokenBlacklistService tokenBlacklistService;

    // ── blacklist ────────────────────────────────────────────────────────────

    @Test
    void blacklist_validToken_setsRedisKeyWithRemainingTtl() {
        long futureMs = System.currentTimeMillis() + 300_000; // 5 分鐘後
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(jwtUtil.getJtiFromToken("token")).thenReturn("jti-abc");
        when(jwtUtil.getExpirationDateFromToken("token")).thenReturn(new Date(futureMs));

        tokenBlacklistService.blacklist("token");

        verify(valueOps).set(eq("blacklist:jti-abc"), eq("1"), any(Duration.class));
    }

    @Test
    void blacklist_expiredToken_skipsRedisWrite() {
        long pastMs = System.currentTimeMillis() - 1000; // 1 秒前已過期
        when(jwtUtil.getJtiFromToken("old-token")).thenReturn("jti-xyz");
        when(jwtUtil.getExpirationDateFromToken("old-token")).thenReturn(new Date(pastMs));

        tokenBlacklistService.blacklist("old-token");

        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void blacklist_jtiIsNull_skipsRedisWrite() {
        when(jwtUtil.getJtiFromToken("bad-token")).thenReturn(null);

        tokenBlacklistService.blacklist("bad-token");

        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void blacklist_exceptionFromJwtUtil_doesNotPropagate() {
        when(jwtUtil.getJtiFromToken(any())).thenThrow(new RuntimeException("parse error"));

        tokenBlacklistService.blacklist("malformed"); // 不應拋例外
    }

    // ── revokeAllForUser ─────────────────────────────────────────────────────

    @Test
    void revokeAllForUser_storesRevokeTime_withAccessTokenLifetimeAsTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(jwtUtil.getAccessExpirationSeconds()).thenReturn(1800L);
        long before = System.currentTimeMillis();

        tokenBlacklistService.revokeAllForUser(7L);

        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(eq("user_revoked_at:7"), value.capture(), eq(Duration.ofSeconds(1800)));
        assertThat(Long.parseLong(value.getValue())).isBetween(before, System.currentTimeMillis());
    }

    // ── isRevoked ────────────────────────────────────────────────────────────

    @Test
    void isRevoked_notBlacklistedAndNoRevokeRecord_returnsFalse() {
        stubRedis(null, null);

        assertThat(tokenBlacklistService.isRevoked(claims(10_000))).isFalse();
    }

    @Test
    void isRevoked_jtiBlacklisted_returnsTrue() {
        stubRedis("1", null);

        assertThat(tokenBlacklistService.isRevoked(claims(10_000))).isTrue();
    }

    @Test
    void isRevoked_issuedBeforeRevoke_returnsTrue() {
        stubRedis(null, "12000");

        assertThat(tokenBlacklistService.isRevoked(claims(10_000))).isTrue();
    }

    @Test
    void isRevoked_issuedInSameSecondAsRevoke_returnsTrue() {
        // iat 只到秒: 10.000 秒發出的 token 可能是 10.000~10.999 之間任何時間, 撤銷在 10.500 時保守視為無效
        stubRedis(null, "10500");

        assertThat(tokenBlacklistService.isRevoked(claims(10_000))).isTrue();
    }

    @Test
    void isRevoked_issuedAfterRevoke_returnsFalse() {
        stubRedis(null, "10500");

        assertThat(tokenBlacklistService.isRevoked(claims(11_000))).isFalse();
    }

    @Test
    void isRevoked_redisUnavailable_failsClosed() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.multiGet(any())).thenThrow(new RuntimeException("connection refused"));

        assertThat(tokenBlacklistService.isRevoked(claims(10_000))).isTrue();
    }

    @Test
    void isRevoked_tokenWithoutUserId_returnsTrue_withoutRedisLookup() {
        Claims claims = Jwts.claims().id("jti-1").issuedAt(new Date(10_000)).build();

        assertThat(tokenBlacklistService.isRevoked(claims)).isTrue();
        verify(redisTemplate, never()).opsForValue();
    }

    private void stubRedis(String blacklisted, String revokedAt) {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.multiGet(List.of("blacklist:jti-1", "user_revoked_at:7")))
                .thenReturn(Arrays.asList(blacklisted, revokedAt));
    }

    // 與 JwtUtil 簽發的一樣: userId 解析回來是 Integer
    private Claims claims(long issuedAtMs) {
        return Jwts.claims().id("jti-1").issuedAt(new Date(issuedAtMs)).add(JwtUtil.CLAIM_USER_ID, 7).build();
    }
}
