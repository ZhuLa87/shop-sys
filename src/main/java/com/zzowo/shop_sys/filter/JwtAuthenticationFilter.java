package com.zzowo.shop_sys.filter;

import lombok.RequiredArgsConstructor;
import com.zzowo.shop_sys.enums.Role;
import com.zzowo.shop_sys.service.TokenBlacklistService;
import com.zzowo.shop_sys.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = JwtUtil.TOKEN_TYPE + " ";

    private final JwtUtil jwtUtil;

    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 每個請求只驗簽解析一次; 簽章錯誤或已過期時 parseClaims 回傳 empty, 請求以未認證身分繼續
            jwtUtil.parseClaims(authHeader.substring(BEARER_PREFIX.length()))
                    .ifPresent(claims -> authenticate(claims, request));
        }

        chain.doFilter(request, response);
    }

    // 從 JWT claims 直接取得身分與角色,不查資料庫;同時檢查 token 是否已失效 (已登出, 或帳號異動後被撤銷)
    private void authenticate(Claims claims, HttpServletRequest request) {
        String email = claims.getSubject();
        String role = claims.get(JwtUtil.CLAIM_ROLE, String.class);
        if (email == null || role == null || tokenBlacklistService.isRevoked(claims)) {
            return;
        }

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority(Role.AUTHORITY_PREFIX + role)));

        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}