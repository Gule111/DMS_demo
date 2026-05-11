package com.dms.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dms.common.JwtUtils;
import com.dms.entity.User;
import com.dms.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务
 * 负责处理登录认证、Token 管理
 */
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final SmsCodeService smsCodeService;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;

    @Value("${dms.redis.token-key-prefix}")
    private String tokenKeyPrefix;

    public AuthService(UserMapper userMapper, SmsCodeService smsCodeService,
                       JwtUtils jwtUtils, StringRedisTemplate redisTemplate) {
        this.userMapper = userMapper;
        this.smsCodeService = smsCodeService;
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Login with phone + password + SMS code
     */
    public Map<String, Object> loginByPhone(String phone, String password, String code) {
        // 1. Verify SMS code
        boolean verified = smsCodeService.verifyCode(phone, code);
        if (!verified) {
            throw new RuntimeException("Verification code is wrong or expired");
        }

        // 2. Find user by phone
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new RuntimeException("This phone number is not registered");
        }

        // 3. Verify password (MD5)
        String hashedPassword = DigestUtil.md5Hex(password);
        if (!hashedPassword.equals(user.getPassword())) {
            throw new RuntimeException("Incorrect password");
        }

        // 3. 生成 JWT Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 4. 将 Token 存入 Redis（用于主动注销和续期管理）
        String redisKey = tokenKeyPrefix + user.getId();
        redisTemplate.opsForValue().set(redisKey, token,
                jwtUtils.getExpiration(), TimeUnit.MILLISECONDS);

        // 5. 组装返回信息
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("phone", user.getPhone());

        return result;
    }

    /**
     * Register new user (username + phone + password + SMS code)
     */
    public Map<String, Object> register(String username, String phone, String password, String code) {
        // 1. Verify SMS code
        boolean verified = smsCodeService.verifyCode(phone, code);
        if (!verified) {
            throw new RuntimeException("Verification code is wrong or expired");
        }

        // 2. Check if phone is already registered
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        User existUser = userMapper.selectOne(queryWrapper);
        if (existUser != null) {
            throw new RuntimeException("This phone number is already registered");
        }

        // 3. Create new user with MD5 hashed password
        User user = new User();
        user.setUsername(username);
        user.setPhone(phone);
        user.setPassword(DigestUtil.md5Hex(password));
        user.setRole(1); // Default role: student
        userMapper.insert(user);

        // 4. 注册成功后自动登录，生成 Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 5. Token 存入 Redis
        String redisKey = tokenKeyPrefix + user.getId();
        redisTemplate.opsForValue().set(redisKey, token,
                jwtUtils.getExpiration(), TimeUnit.MILLISECONDS);

        // 6. 组装返回信息
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("phone", user.getPhone());

        return result;
    }

    /**
     * 退出登录 —— 从 Redis 中删除 Token
     *
     * @param userId 用户ID
     */
    public void logout(Long userId) {
        String redisKey = tokenKeyPrefix + userId;
        redisTemplate.delete(redisKey);
    }

    /**
     * 检查 Token 在 Redis 中是否仍然有效（未被主动注销）
     *
     * @param userId 用户ID
     * @param token  JWT Token
     * @return true=有效, false=已注销
     */
    public boolean isTokenValid(Long userId, String token) {
        String redisKey = tokenKeyPrefix + userId;
        String cachedToken = redisTemplate.opsForValue().get(redisKey);
        return token.equals(cachedToken);
    }
}
