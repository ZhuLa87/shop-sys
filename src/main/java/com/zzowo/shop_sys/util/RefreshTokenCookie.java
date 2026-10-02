package com.zzowo.shop_sys.util;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

// Refresh Token 只以 HttpOnly cookie 傳遞, 不出現在 response body, XSS 讀不到
// - Path=/: SSR 時瀏覽器對頁面的請求也要帶上, Nitro 才能在伺服器端代為 refresh
// - SameSite=Lax: 跨站 POST 不會夾帶, /auth/refresh 與 /auth/logout 不會被 CSRF
// - 不設 Max-Age: 和 auth_token 一樣是 session cookie, 實際期限由 Redis TTL 控制
@Component
public class RefreshTokenCookie {

    public static final String NAME = "refresh_token";

    public ResponseCookie issue(String token) {
        return base(token).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/");
    }
}
