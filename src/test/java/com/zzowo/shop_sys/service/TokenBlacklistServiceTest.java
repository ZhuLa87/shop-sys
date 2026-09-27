package com.zzowo.shop_sys.service;

import com.zzowo.shop_sys.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Date;

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

    // ── isBlacklisted ────────────────────────────────────────────────────────

    @Test
    void isBlacklistedJti_jtiInRedis_returnsTrue() {
        when(redisTemplate.hasKey("blacklist:jti-blocked")).thenReturn(Boolean.TRUE);

        assertThat(tokenBlacklistService.isBlacklistedJti("jti-blocked")).isTrue();
    }

    @Test
    void isBlacklistedJti_jtiNotInRedis_returnsFalse() {
        when(redisTemplate.hasKey("blacklist:jti-clean")).thenReturn(Boolean.FALSE);

        assertThat(tokenBlacklistService.isBlacklistedJti("jti-clean")).isFalse();
    }

    @Test
    void isBlacklistedJti_redisUnavailable_failsClosed() {
        when(redisTemplate.hasKey("blacklist:jti-1")).thenThrow(new RuntimeException("connection refused"));

        assertThat(tokenBlacklistService.isBlacklistedJti("jti-1")).isTrue();
    }

    @Test
    void isBlacklistedJti_null_returnsFalse() {
        assertThat(tokenBlacklistService.isBlacklistedJti(null)).isFalse();
        verify(redisTemplate, never()).hasKey(anyString());
    }
}
