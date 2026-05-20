package com.zzowo.shop_sys.util;

import com.zzowo.shop_sys.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

@Component
public class JwtUtil {

    // 從 application.yaml 讀取設定
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * 生成密鑰
     *
     * 取得簽名用的 Key (因為 HMAC-SHA 需要至少 256 bit 的密鑰,這裡做一點處理)
     */
    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 從token中獲取用戶名
     */
    public String getUsernameFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        try {
            return getClaimFromToken(token, Claims::getSubject);
        } catch (Exception e) {
            // JWT 解析失敗
            return null;
        }
    }

    /**
     * 從 token 取得過期時間.
     * 即使 token 已過期 (ExpiredJwtException) 也能正確回傳,供黑名單計算 TTL 使用.
     */
    public Date getExpirationDateFromToken(String token) {
        try {
            return getClaimFromToken(token, Claims::getExpiration);
        } catch (ExpiredJwtException e) {
            return e.getClaims().getExpiration();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 從 token 取得 jti (JWT ID) .
     * 即使 token 已過期也能回傳,供登出時將 token 加入黑名單使用.
     */
    public String getJtiFromToken(String token) {
        try {
            return getClaimFromToken(token, Claims::getId);
        } catch (ExpiredJwtException e) {
            return e.getClaims().getId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 從token中獲取特定聲明
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 從token中獲取所有聲明
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 檢查token是否過期
     */
    private Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration != null && expiration.before(new Date());
        } catch (Exception e) {
            return true; // 如果無法解析,視為過期
        }
    }

    /**
     * 生成token (將 userId 與 role 寫入 claims,讓 Filter 無需查詢資料庫) 
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        return createToken(claims, user.getUsername());
    }

    public Long getUserIdFromToken(String token) {
        return getClaimFromToken(token, claims -> ((Number) claims.get("userId")).longValue());
    }

    public String getRoleFromToken(String token) {
        return getClaimFromToken(token, claims -> (String) claims.get("role"));
    }

    /**
     * 僅驗證 token 結構與有效期,不需要查詢資料庫
     */
    public Boolean validateToken(String token) {
        try {
            getAllClaimsFromToken(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public long getAccessExpirationSeconds() {
        return expiration / 1000;
    }

    /**
     * 創建token,每個 token 附帶唯一 jti (供黑名單使用) 
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSignKey())
                .compact();
    }

}
