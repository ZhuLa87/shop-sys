package com.zzowo.shop_sys.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.zzowo.shop_sys.filter.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

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
                        // 1. 公開端點
                        // 允許 "註冊" 和 "登入"
                        .requestMatchers("/v1/auth/**").permitAll()
                        // 允許商品瀏覽端點
                        .requestMatchers(HttpMethod.GET, "/v1/products/**").permitAll()

                        // 2. 管理員端點
                        // 只有 產品經理 或 超級管理員 可以對 /v1/products/** 進行 POST/PUT/DELETE
                        .requestMatchers(HttpMethod.POST, "/v1/products/**").hasAnyRole("PRODUCT_MANAGER", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/v1/products/**").hasAnyRole("PRODUCT_MANAGER", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/v1/products/**")
                        .hasAnyRole("PRODUCT_MANAGER", "SUPER_ADMIN")

                        // 3. 使用者管理端點
                        .requestMatchers(HttpMethod.GET, "/v1/users/me").authenticated() // 取得自己
                        .requestMatchers(HttpMethod.PUT, "/v1/users/me").authenticated() // 更新自己
                        .requestMatchers(HttpMethod.PUT, "/v1/users/{id}").hasRole("SUPER_ADMIN") // 超級管理員更新特定用戶
                        .requestMatchers(HttpMethod.GET, "/v1/users/{id}").hasRole("SUPER_ADMIN") // 超級管理員取得特定用戶
                        .requestMatchers(HttpMethod.GET, "/v1/users").hasRole("SUPER_ADMIN") // 超級管理員取得所有用戶

                        // 4. 其他所有請求都需要登入才能看
                        .anyRequest().authenticated()
            )
            // 設定為無狀態 (Stateless), 因為我們用 JWT，伺服器不需要存 Session
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 把過濾器加在 UsernamePasswordAuthenticationFilter 之前。先檢查 JWT，如果沒有 JWT 才走傳統流程 (但這裡其實只靠 JWT)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}