package com.zzowo.shop_sys.util;

import com.zzowo.shop_sys.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

@Component
public class JwtUtil {

    public static final String TOKEN_TYPE = "Bearer";
    public static final String CLAIM_ROLE = "role";

    // 從 application.yaml 讀取設定
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey signKey;

    // 啟動時建立一次簽名用的 Key; JWT_SECRET 太短 (HMAC-SHA 至少需要 256 bit) 會在這裡直接啟動失敗,
    // 而不是等到第一個請求才出錯
    @PostConstruct
    void init() {
        signKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getSignKey() {
        return signKey;
    }

    /**
     * 驗證簽章與有效期並取出 claims. 簽章錯誤, 格式錯誤或已過期都回傳 empty.
     * 供 JwtAuthenticationFilter 每個請求只解析一次 token.
     */
    public Optional<Claims> parseClaims(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(getAllClaimsFromToken(token));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
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
     * 生成token (將 userId 與 role 寫入 claims,讓 Filter 無需查詢資料庫) 
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put(CLAIM_ROLE, user.getRole().name());
        return createToken(claims, user.getUsername());
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
