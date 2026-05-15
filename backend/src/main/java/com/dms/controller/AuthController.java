package com.dms.controller;

import com.dms.common.Result;
import com.dms.service.AuthService;
import com.dms.service.SmsCodeService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 * 提供发送验证码、登录、退出登录等接口
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final SmsCodeService smsCodeService;
    private final AuthService authService;

    public AuthController(SmsCodeService smsCodeService, AuthService authService) {
        this.smsCodeService = smsCodeService;
        this.authService = authService;
    }

    /**
     * 发送短信验证码
     * POST /auth/sendCode
     * Body: { "phone": "13700137001" }
     */
    @PostMapping("/sendCode")
    public Result<String> sendCode(@RequestBody Map<String, String> params) {
        String phone = params.get("phone");
        if (phone == null || phone.isBlank()) {
            return Result.error("手机号不能为空");
        }

        // 简单的手机号格式校验
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            return Result.error("手机号格式不正确");
        }

        String code = smsCodeService.sendCode(phone);

        // 测试模式下返回验证码，生产环境应移除此行
        return Result.success("验证码已发送（测试模式，验证码: " + code + "）");
    }

    /**
     * 手机号 + 验证码 登录
     * POST /auth/login
     * Body: { "phone": "13700137001", "code": "123456" }
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String phone = params.get("phone");
        String password = params.get("password");
        String code = params.get("code");

        if (phone == null || phone.isBlank()) {
            return Result.error("Phone is required");
        }
        if (password == null || password.isBlank()) {
            return Result.error("Password is required");
        }
        if (code == null || code.isBlank()) {
            return Result.error("Code is required");
        }

        try {
            Map<String, Object> loginResult = authService.loginByPhone(phone, password, code);
            return Result.success(loginResult);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 注册新用户
     * POST /auth/register
     * Body: { "username": "张三", "phone": "13700137003", "code": "123456" }
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String phone = params.get("phone");
        String password = params.get("password");
        String code = params.get("code");

        if (username == null || username.isBlank()) {
            return Result.error("Username is required");
        }
        if (phone == null || phone.isBlank()) {
            return Result.error("Phone is required");
        }
        if (password == null || password.isBlank()) {
            return Result.error("Password is required");
        }
        if (code == null || code.isBlank()) {
            return Result.error("Code is required");
        }

        try {
            Map<String, Object> registerResult = authService.register(username, phone, password, code);
            return Result.success(registerResult);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 刷新 Token 接口
     * POST /auth/refresh
     * Body: { "refreshToken": "..." }
     */
    @PostMapping("/refresh")
    public Result<String> refresh(@RequestBody Map<String, String> params) {
        String refreshToken = params.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return Result.error("Refresh Token 不能为空");
        }
        try {
            String newAccessToken = authService.refreshToken(refreshToken);
            return Result.success(newAccessToken);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 退出登录
     * POST /auth/logout
     * 需要在请求头中携带 Authorization: Bearer {token}
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        authService.logout(userId);
        return Result.success();
    }

    /**
     * 获取当前登录用户信息（测试接口）
     * GET /auth/info
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();
        Map<String, Object> info = Map.of(
                "userId", userId,
                "authorities", authentication.getAuthorities()
        );
        return Result.success(info);
    }
}
