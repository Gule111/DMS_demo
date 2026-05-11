package com.dms.config;

import com.dms.common.JwtUtils;
import com.dms.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 认证过滤器
 * 每次请求都会执行，从请求头中提取 Token 并验证
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final AuthService authService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, AuthService authService) {
        this.jwtUtils = jwtUtils;
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 从请求头中提取 Token
        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                // 2. 解析 Token 获取用户信息
                Long userId = jwtUtils.getUserId(token);
                String username = jwtUtils.getUsername(token);
                Integer role = jwtUtils.getRole(token);

                // 3. 检查 Token 是否在 Redis 中仍然有效（未被注销）
                if (!authService.isTokenValid(userId, token)) {
                    writeErrorResponse(response, 401, "Token已失效，请重新登录");
                    return;
                }

                // 4. 构建权限信息并设置到 SecurityContext 中
                String roleStr = switch (role) {
                    case 1 -> "ROLE_ADMIN";
                    case 2 -> "ROLE_COACH";
                    case 3 -> "ROLE_STUDENT";
                    default -> "ROLE_USER";
                };

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId, null,
                                Collections.singletonList(new SimpleGrantedAuthority(roleStr))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (ExpiredJwtException e) {
                writeErrorResponse(response, 401, "Token已过期，请重新登录");
                return;
            } catch (Exception e) {
                writeErrorResponse(response, 401, "Token无效");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从 Authorization 请求头中提取 Bearer Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 向客户端写入错误响应（JSON 格式）
     */
    private void writeErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("code", status);
        result.put("message", message);
        response.getWriter().write(new ObjectMapper().writeValueAsString(result));
    }
}
