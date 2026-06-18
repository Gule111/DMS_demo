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
        // 1. 从 HTTP 请求的 Authorization 头中提取 Bearer Token
        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                // 2. 验证并解析 Token 签名，提取载荷中的明文用户信息
                Long userId = jwtUtils.getUserId(token);
                String username = jwtUtils.getUsername(token);
                Integer role = jwtUtils.getRole(token); // (1-管理员, 2-教练员, 3-学员)

                // 3. 核心安全防护：比对 Redis 缓存中的活跃 Token，验证令牌是否依然有效
                // 这在以下场景下发挥决定性作用：
                //   - 修改角色后踢人：当管理员修改角色，Redis 中该用户的 Token 会被 logout() 删掉，此处就会返回 false 并阻断访问。
                //   - 单点登录/顶号下线：若用户在别处登录，原 Token 被新 Token 覆盖，原 Token 在此处就会失效，返回 401。
                if (!authService.isTokenValid(userId, token)) {
                    writeErrorResponse(response, 401, "Token已失效，请重新登录");
                    return;
                }

                // 4. 根据解密出的 role id，映射到 Spring Security 标准的权限字符串
                // 该角色权限会在后端 Controller 层的 @PreAuthorize 注解中被进一步强制检验，构成终极防线。
                String roleStr = switch (role) {
                    case 1 -> "ROLE_ADMIN";
                    case 2 -> "ROLE_COACH";
                    case 3 -> "ROLE_STUDENT";
                    default -> "ROLE_USER";
                };

                // 5. 将安全校验通过的用户身份与权限角色封装注册到 Security 上下文中，供本次请求后续使用
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId, null,
                                Collections.singletonList(new SimpleGrantedAuthority(roleStr))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (ExpiredJwtException e) {
                // 捕获 JWT 超过设定的过期期限异常
                writeErrorResponse(response, 401, "Token已过期，请重新登录");
                return;
            } catch (Exception e) {
                // 捕获签名不匹配、非法伪造篡改等引起的解析失败异常
                writeErrorResponse(response, 401, "Token无效");
                return;
            }
        }

        // 放行请求，继续执行过滤器链
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
