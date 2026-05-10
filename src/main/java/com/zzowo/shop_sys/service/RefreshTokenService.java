package com.zzowo.shop_sys.service;

import com.zzowo.shop_sys.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class RefreshTokenService {

    private static final String RT_PREFIX = "refresh_token:"; // token → userId
    private static final String UR_PREFIX = "user_refresh:";  // userId → token（反查索引）

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 為指定使用者建立新的 Refresh Token。
     * 每位使用者只保留一個有效 Token，舊的會被自動刪除。
     *
     * @return 新的 Refresh Token 字串
     */
    public String create(Long userId) {
        // 先刪除該使用者的舊 Token（如有）
        String oldToken = redisTemplate.opsForValue().get(UR_PREFIX + userId);
        if (oldToken != null) {
            redisTemplate.delete(RT_PREFIX + oldToken);
        }

        String token = UUID.randomUUID().toString();
        Duration ttl = Duration.ofMillis(refreshExpirationMs);

        redisTemplate.opsForValue().set(RT_PREFIX + token, String.valueOf(userId), ttl);
        redisTemplate.opsForValue().set(UR_PREFIX + userId, token, ttl);

        return token;
    }

    /**
     * 驗證 Refresh Token 並刪除（Token Rotation 前的第一步）。
     *
     * @return 對應的 userId，供呼叫方重新發 Access Token 與新 Refresh Token
     * @throws BusinessException 若 Token 無效或已過期
     */
    public Long validateAndDelete(String token) {
        String userIdStr = redisTemplate.opsForValue().get(RT_PREFIX + token);
        if (userIdStr == null) {
            throw new BusinessException("無效或已過期的 Refresh Token，請重新登入");
        }
        Long userId = Long.parseLong(userIdStr);

        redisTemplate.delete(RT_PREFIX + token);
        redisTemplate.delete(UR_PREFIX + userId);

        return userId;
    }

    /**
     * 靜默刪除 Refresh Token（登出時使用，不拋例外）。
     */
    public void deleteIfExists(String token) {
        String userIdStr = redisTemplate.opsForValue().get(RT_PREFIX + token);
        if (userIdStr != null) {
            redisTemplate.delete(UR_PREFIX + userIdStr);
        }
        redisTemplate.delete(RT_PREFIX + token);
    }
}
