package com.zzowo.shop_sys.service;

import com.zzowo.shop_sys.entity.RefreshToken;
import com.zzowo.shop_sys.entity.User;
import com.zzowo.shop_sys.exception.BusinessException;
import com.zzowo.shop_sys.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public RefreshToken create(User user) {
        // 每位使用者只保留一個有效 Refresh Token，登入時先刪舊的
        refreshTokenRepository.deleteByUser(user);

        RefreshToken rt = new RefreshToken();
        rt.setToken(UUID.randomUUID().toString());
        rt.setUser(user);
        rt.setExpiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000));
        return refreshTokenRepository.save(rt);
    }

    /**
     * 驗證並輪替（Rotate）：驗證後刪除舊 Token，由呼叫方負責發新的。
     */
    @Transactional
    public RefreshToken validateAndRotate(String tokenStr) {
        RefreshToken rt = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new BusinessException("無效的 Refresh Token"));

        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(rt);
            throw new BusinessException("Refresh Token 已過期，請重新登入");
        }

        // 刪除舊 Token，讓呼叫方 create() 一個新的（Token Rotation）
        refreshTokenRepository.delete(rt);
        return rt;
    }

    @Transactional
    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
