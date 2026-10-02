package com.zzowo.shop_sys.service;

import lombok.RequiredArgsConstructor;
import com.zzowo.shop_sys.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BL_PREFIX = "blacklist:";
    private static final String REVOKED_AT_PREFIX = "user_revoked_at:"; // userId -> 撤銷時間 (epoch 毫秒)

    private final StringRedisTemplate redisTemplate;

    private final JwtUtil jwtUtil;

    /**
     * 將 Access Token 加入黑名單,TTL 設為 Token 剩餘有效時間.
     * 若 Token 已過期或無法解析則直接跳過 (不需要加入黑名單) .
     */
    public void blacklist(String token) {
        try {
            String jti = jwtUtil.getJtiFromToken(token);
            Date expiration = jwtUtil.getExpirationDateFromToken(token);

            if (jti == null || expiration == null) return;

            long ttlMs = expiration.getTime() - System.currentTimeMillis();
            if (ttlMs <= 0) return; // 已過期,filter 自然會擋,不需要加入黑名單

            redisTemplate.opsForValue().set(BL_PREFIX + jti, "1", Duration.ofMillis(ttlMs));
        } catch (Exception e) {
            log.warn("Failed to blacklist token: {}", e.getMessage());
        }
    }

    /**
     * 撤銷使用者目前所有的 Access Token: 記下撤銷時間, 之前發出的 token 一律視為無效.
     * TTL 等於 Access Token 效期, 過了之後撤銷前發的 token 本來就過期了.
     */
    public void revokeAllForUser(Long userId) {
        redisTemplate.opsForValue().set(REVOKED_AT_PREFIX + userId, String.valueOf(System.currentTimeMillis()),
                Duration.ofSeconds(jwtUtil.getAccessExpirationSeconds()));
    }

    /**
     * 檢查已驗簽的 token 是否失效: jti 在黑名單 (已登出), 或發出時間不晚於該使用者的撤銷時間
     * (停用, 改角色, 改密碼, 改 Email). 兩個 key 一次 multiGet 查完.
     * iat 只精確到秒, 撤銷那一秒內發出的 token 一律視為無效 (撤銷後同一秒內重新登入的也算, 再登入一次即可).
     * 缺少 jti, userId 或 iat 的 token 不是本系統發的, 同樣視為無效.
     * Redis 無法查詢時視為已失效 (fail-closed): 登出與撤銷是安全功能, Redis 故障期間
     * 不能讓已失效的 token 重新變成有效. Refresh 本來就依賴 Redis, 故障時系統也無法正常運作.
     */
    public boolean isRevoked(Claims claims) {
        String jti = claims.getId();
        Number userId = claims.get(JwtUtil.CLAIM_USER_ID, Number.class);
        Date issuedAt = claims.getIssuedAt();
        if (jti == null || userId == null || issuedAt == null) {
            return true;
        }
        try {
            List<String> values = redisTemplate.opsForValue().multiGet(
                    List.of(BL_PREFIX + jti, REVOKED_AT_PREFIX + userId.longValue()));
            if (values == null) {
                return true;
            }
            if (values.get(0) != null) {
                return true;
            }
            String revokedAt = values.get(1);
            return revokedAt != null && issuedAt.getTime() <= Long.parseLong(revokedAt);
        } catch (Exception e) {
            log.warn("無法查詢 token 撤銷狀態,視為已失效: {}", e.getMessage());
            return true;
        }
    }
}
