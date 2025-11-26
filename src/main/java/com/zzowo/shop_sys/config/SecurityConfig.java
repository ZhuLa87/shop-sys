package com.zzowo.shop_sys.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    // 2. 設定基本的網頁安全規則 (SecurityFilterChain)
    // 因為我們剛加入 Security，如果不設定這個，所有功能(包含註冊)都會預設被擋住需要登入
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 暫時關閉 CSRF 防護 (因為我們之後要用 JWT，且目前是前後端分離，先關閉比較好測試)
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // 允許 "註冊" 和 "登入" 的 API 不需要登入就能訪問
                .requestMatchers("/auth/**").permitAll()
                // 其他所有請求都需要登入才能看
                .anyRequest().authenticated()
            );

        return http.build();
    }
}