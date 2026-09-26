package com.zzowo.shop_sys.service;

import lombok.RequiredArgsConstructor;
import com.zzowo.shop_sys.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BL_PREFIX = "blacklist:";

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
     * 檢查 Access Token 是否在黑名單中.
     * Redis 無法查詢時視為已列入黑名單 (fail-closed): 登出是安全功能, Redis 故障期間
     * 不能讓已登出的 token 重新變成有效. Refresh 本來就依賴 Redis, 故障時系統也無法正常運作.
     */
    public boolean isBlacklisted(String token) {
        try {
            String jti = jwtUtil.getJtiFromToken(token);
            if (jti == null) return false;
            return Boolean.TRUE.equals(redisTemplate.hasKey(BL_PREFIX + jti));
        } catch (Exception e) {
            log.warn("無法查詢 token 黑名單,視為已失效: {}", e.getMessage());
            return true;
        }
    }
}
