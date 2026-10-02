package com.zzowo.shop_sys.event;

// 帳號異動 (停用, 改角色, 改密碼, 改 Email) 後要讓該使用者已發出的 token 全部失效.
// 由 AuthService 在 transaction commit 之後處理
public record UserSessionsRevokedEvent(Long userId) {
}
