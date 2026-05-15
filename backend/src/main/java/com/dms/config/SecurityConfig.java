package com.dms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Security 核心配置
 * - 基于 JWT 的无状态认证
 * - 关闭 CSRF（前后端分离不需要）
 * - 配置白名单路径（登录/注册/发送验证码等）
 * - 配置 CORS 跨域
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 开启方法级别权限注解 (@PreAuthorize)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 关闭 CSRF（前后端分离项目不需要）
                .csrf(AbstractHttpConfigurer::disable)

                // 配置 CORS 跨域
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 基于 Token，不需要 Session
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 配置请求授权规则
                .authorizeHttpRequests(auth -> auth
                        // 白名单：认证相关接口放行
                        .requestMatchers(
                                "/auth/sendCode",
                                "/auth/login",
                                "/auth/register",
                                "/auth/refresh"
                        ).permitAll()
                        // 其他所有请求需要认证
                        .anyRequest().authenticated()
                )

                // 自定义未认证/未授权的返回格式
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            Map<String, Object> result = new HashMap<>();
                            result.put("code", 401);
                            result.put("message", "未登录或登录已过期，请先登录");
                            response.getWriter().write(new ObjectMapper().writeValueAsString(result));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            Map<String, Object> result = new HashMap<>();
                            result.put("code", 403);
                            result.put("message", "权限不足，无法访问该资源");
                            response.getWriter().write(new ObjectMapper().writeValueAsString(result));
                        })
                )

                // 将 JWT 过滤器添加到 UsernamePasswordAuthenticationFilter 之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 跨域配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000")); // 前端地址
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
