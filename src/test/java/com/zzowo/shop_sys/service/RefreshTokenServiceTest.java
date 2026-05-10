package com.zzowo.shop_sys.service;

import com.zzowo.shop_sys.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    StringRedisTemplate redisTemplate;

    @Mock
    ValueOperations<String, String> valueOps;

    @InjectMocks
    RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 604800000L);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_noExistingToken_storesBothKeys() {
        when(valueOps.get("user_refresh:1")).thenReturn(null);

        String token = refreshTokenService.create(1L);

        assertThat(token).isNotBlank();
        verify(valueOps).set(eq("refresh_token:" + token), eq("1"), any(Duration.class));
        verify(valueOps).set(eq("user_refresh:1"), eq(token), any(Duration.class));
    }

    @Test
    void create_existingToken_deletesOldBeforeStoringNew() {
        when(valueOps.get("user_refresh:1")).thenReturn("old-token-uuid");

        refreshTokenService.create(1L);

        verify(redisTemplate).delete("refresh_token:old-token-uuid");
    }

    @Test
    void create_ttlEqualsRefreshExpiration() {
        when(valueOps.get("user_refresh:1")).thenReturn(null);

        String token = refreshTokenService.create(1L);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOps).set(eq("refresh_token:" + token), anyString(), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofMillis(604800000L));
    }

    // ── validateAndDelete ────────────────────────────────────────────────────

    @Test
    void validateAndDelete_validToken_returnsUserIdAndDeletesBothKeys() {
        when(valueOps.get("refresh_token:valid-token")).thenReturn("42");

        Long userId = refreshTokenService.validateAndDelete("valid-token");

        assertThat(userId).isEqualTo(42L);
        verify(redisTemplate).delete("refresh_token:valid-token");
        verify(redisTemplate).delete("user_refresh:42");
    }

    @Test
    void validateAndDelete_tokenNotInRedis_throwsBusinessException() {
        when(valueOps.get("refresh_token:ghost-token")).thenReturn(null);

        assertThatThrownBy(() -> refreshTokenService.validateAndDelete("ghost-token"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("無效或已過期");
    }

    // ── deleteIfExists ───────────────────────────────────────────────────────

    @Test
    void deleteIfExists_existingToken_deletesBothKeys() {
        when(valueOps.get("refresh_token:known-token")).thenReturn("7");

        refreshTokenService.deleteIfExists("known-token");

        verify(redisTemplate).delete("refresh_token:known-token");
        verify(redisTemplate).delete("user_refresh:7");
    }

    @Test
    void deleteIfExists_unknownToken_deletesTokenKeyOnly_noException() {
        when(valueOps.get("refresh_token:unknown")).thenReturn(null);

        refreshTokenService.deleteIfExists("unknown"); // 不應拋例外

        // capture the single delete call and assert only the token key was deleted
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate, times(1)).delete(captor.capture());
        assertThat(captor.getValue()).isEqualTo("refresh_token:unknown");
    }
}
