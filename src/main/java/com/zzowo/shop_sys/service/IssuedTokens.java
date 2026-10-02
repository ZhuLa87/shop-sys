package com.zzowo.shop_sys.service;

// AuthService 核發的一組 token. refreshToken 只交給 controller 寫入 HttpOnly cookie, 不放進 response body
public record IssuedTokens(String accessToken, String refreshToken, long expiresIn) {
}
