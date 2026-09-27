package com.zzowo.shop_sys.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzowo.shop_sys.dto.response.ApiResponse;
import com.zzowo.shop_sys.filter.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 使用 Spring 管理的 ObjectMapper, 401/403 回應才會套用與一般 API 相同的序列化設定
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    // 供 UserService 登入時使用,底層由 Spring Boot 自動組出 DaoAuthenticationProvider (CustomUserDetailsService + passwordEncoder)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // URL 層級的授權規則; 規則由上而下比對, 第一個符合的生效
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 純 Bearer token API: 瀏覽器不會自動夾帶 Authorization header, 沒有 CSRF 的攻擊面
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // 1. 公開端點
                        // 允許 "註冊" 和 "登入"
                        .requestMatchers("/v1/auth/**").permitAll()

                        // Swagger UI
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                        // Actuator health:供 docker healthcheck 與反向代理探測用
                        // 實際路徑為 /api/health (context-path 之後的部分才進到這裡比對) ,
                        // 且 show-details 設為 when-authorized,匿名請求只會看到 {"status":"UP"}
                        .requestMatchers("/health", "/health/**").permitAll()

                        // 取得商品庫存變動紀錄
                        .requestMatchers(HttpMethod.GET, "/v1/products/*/inventory-logs").hasAnyRole("PRODUCT_MANAGER", "SUPER_ADMIN")
                        // 查看所有商品紀錄總覽
                        .requestMatchers(HttpMethod.GET, "/v1/products/inventory-logs").hasAnyRole("PRODUCT_MANAGER", "SUPER_ADMIN")

                        // 已刪除商品清單 / 管理員商品列表:僅管理員可見,必須在 permitAll 萬用規則之前宣告
                        .requestMatchers(HttpMethod.GET, "/v1/products/deleted").hasAnyRole("PRODUCT_MANAGER", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/v1/products/admin").hasAnyRole("PRODUCT_MANAGER", "SUPER_ADMIN")

                        // 允許商品瀏覽端點
                        .requestMatchers(HttpMethod.GET, "/v1/products/**").permitAll()

                        // 2. 管理員端點
                        // 只有 產品經理 或 超級管理員 可以對 /v1/products/** 進行 POST/PUT/DELETE
                        .requestMatchers(HttpMethod.POST, "/v1/products/**").hasAnyRole("PRODUCT_MANAGER", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/v1/products/**").hasAnyRole("PRODUCT_MANAGER", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/v1/products/**").hasAnyRole("PRODUCT_MANAGER", "SUPER_ADMIN")

                        // 3. 使用者管理端點
                        .requestMatchers(HttpMethod.GET, "/v1/users/me").authenticated() // 取得自己
                        .requestMatchers(HttpMethod.PUT, "/v1/users/me").authenticated() // 更新自己
                        .requestMatchers(HttpMethod.PUT, "/v1/users/{id}").hasRole("SUPER_ADMIN") // 超級管理員更新特定用戶
                        .requestMatchers(HttpMethod.GET, "/v1/users/{id}").hasRole("SUPER_ADMIN") // 超級管理員取得特定用戶
                        .requestMatchers(HttpMethod.GET, "/v1/users").hasRole("SUPER_ADMIN") // 超級管理員取得所有用戶

                        // 4. 上傳授權端點 (登入即可,角色細分由 UploadController 內部處理)
                        .requestMatchers(HttpMethod.POST, "/v1/upload/**").authenticated()

                        // 5. 綠界付款回呼:由綠界 server 與消費者瀏覽器直接 POST,不帶 JWT,
                        //    身分改由 CheckMacValue 驗證 (PaymentService.handlePaymentResult)
                        .requestMatchers(HttpMethod.POST, "/v1/payments/ecpay/notify", "/v1/payments/ecpay/result").permitAll()

                        // 6. 其他所有請求都需要登入才能看
                        .anyRequest().authenticated())
                // 無狀態: 身分完全由每個請求的 JWT 決定, 不建立 Session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 在授權判斷之前由 JWT 建立 SecurityContext
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(conf -> conf
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()));

        return http.build();
    }

    // 自定義 401 處理器:回傳 JSON
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ApiResponse<Void> apiResponse = ApiResponse.error("請先登入或提供有效 Token");
            objectMapper.writeValue(response.getOutputStream(), apiResponse);
        };
    }

    // 自定義 403 處理器:回傳 JSON
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ApiResponse<Void> apiResponse = ApiResponse.error("您的權限不足以執行此操作");
            objectMapper.writeValue(response.getOutputStream(), apiResponse);
        };
    }
}