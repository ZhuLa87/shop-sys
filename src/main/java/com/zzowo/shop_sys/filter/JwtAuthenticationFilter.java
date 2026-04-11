package com.zzowo.shop_sys.filter;

import com.zzowo.shop_sys.service.CustomUserDetailsService;
import com.zzowo.shop_sys.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 1. 取得 Header 中的 Authorization 欄位
        // 前端傳過來會是這樣： "Authorization: Bearer eyJhbGciOi..."
        final String authHeader = request.getHeader("Authorization");

        String email = null;
        String jwt = null;

        // 2. 檢查 Header 格式是否正確 (必須以 "Bearer " 開頭)
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7); // 去掉 "Bearer " 前綴，只留 Token
            try {
                // 從 Token 解析出 Email
                // (如果 Token 是偽造的或過期，這裡會拋出異常，我們就捕捉它)
                email = jwtUtil.getUsernameFromToken(jwt);
            } catch (Exception e) {
                logger.error("Token 無效或過期: " + e.getMessage());
            }
        }

        // 3. 驗證並設定身分
        // SecurityContextHolder.getContext().getAuthentication() == null 代表目前還沒登入
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(email);

            // 再次確認 Token 是否有效 (包含過期檢查)
            if (jwtUtil.validateToken(jwt, userDetails)) {

                // 建立 Spring 的驗證物件 (這就像蓋上「已驗證」的章)
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 把驗證物件放進 SecurityContext，這樣後面的 Controller 就知道是誰了！
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 4. 放行，繼續往下一個關卡走
        chain.doFilter(request, response);
    }
}