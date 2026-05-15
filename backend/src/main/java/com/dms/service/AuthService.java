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
    
    private final String refreshTokenKeyPrefix = "dms:refresh_token:";

    public AuthService(UserMapper userMapper, SmsCodeService smsCodeService,
                       JwtUtils jwtUtils, StringRedisTemplate redisTemplate) {
        this.userMapper = userMapper;
        this.smsCodeService = smsCodeService;
        this.jwtUtils = jwtUtils;
        this.redisTemplate = redisTemplate;
    }

    public Map<String, Object> loginByPhone(String phoneOrUsername, String password, String code) {
        // 1. Verify SMS code (bypass if code is 123456 for testing, or if it's admin)
        if (!"123456".equals(code) && !"admin".equals(phoneOrUsername)) {
            boolean verified = smsCodeService.verifyCode(phoneOrUsername, code);
            if (!verified) {
                throw new RuntimeException("Verification code is wrong or expired");
            }
        }

        // 2. Find user by username or phone
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, phoneOrUsername)
                .or()
                .eq(User::getPhone, phoneOrUsername);
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new RuntimeException("该用户不存在");
        }

        // 3. Verify password (MD5)
        String hashedPassword = DigestUtil.md5Hex(password);
        if (!hashedPassword.equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 查询用户角色 (1:管理员, 2:教练员, 3:学员)
        String roleCode = userMapper.getUserRoleCode(user.getId());
        Integer roleId;
        if ("admin".equals(user.getUsername())) {
            roleId = 1; // 强制管理员
        } else if ("instructor".equals(roleCode)) {
            roleId = 2;
        } else if ("student".equals(roleCode)) {
            roleId = 3;
        } else {
            roleId = 3; // 默认学员
        }

        // 4. 生成双 Token (Access + Refresh)
        String accessToken = jwtUtils.generateToken(user.getId(), user.getUsername(), roleId);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername(), roleId);

        // 5. 将 Token 存入 Redis
        // Access Token 存入（主要用于主动注销检查）
        redisTemplate.opsForValue().set(tokenKeyPrefix + user.getId(), accessToken,
                jwtUtils.getExpiration(), TimeUnit.MILLISECONDS);
        // Refresh Token 存入
        redisTemplate.opsForValue().set(refreshTokenKeyPrefix + user.getId(), refreshToken,
                7, TimeUnit.DAYS);

        // 6. 组装返回信息
        Map<String, Object> result = new HashMap<>();
        result.put("token", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", roleId); 
        result.put("phone", user.getPhone());

        return result;
    }

    public Map<String, Object> register(String username, String phone, String password, String code) {
        // 1. Verify SMS code (bypass for testing if code is 123456)
        if (!"123456".equals(code)) {
            boolean verified = smsCodeService.verifyCode(phone, code);
            if (!verified) {
                throw new RuntimeException("Verification code is wrong or expired");
            }
        }

        // 2. Check if username or phone is already registered
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username).or().eq(User::getPhone, phone);
        User existUser = userMapper.selectOne(queryWrapper);
        if (existUser != null) {
            throw new RuntimeException("该用户名或手机号已存在");
        }

        // 3. Create new user with MD5 hashed password
        User user = new User();
        user.setUsername(username); // 用户名和手机号分开了
        user.setPhone(phone);
        user.setPassword(DigestUtil.md5Hex(password));
        user.setStatus(1);
        user.setCreatedAt(java.time.LocalDateTime.now());
        userMapper.insert(user);
        
        // 4. 分配学员角色
        userMapper.insertUserRole(user.getId(), 3L);
        
        // 5. 创建学员业务表记录
        // 身份证在此阶段如果前端没有传，可以默认生成或留空，这里我们用随机字符串顶替
        String fakeIdCard = "ID" + System.currentTimeMillis();
        userMapper.insertBizStudent(user.getId(), username, fakeIdCard, phone);

        // 6. 生成双 Token (3为学员角色ID)
        String accessToken = jwtUtils.generateToken(user.getId(), user.getUsername(), 3);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername(), 3);

        // 7. Token 存入 Redis
        redisTemplate.opsForValue().set(tokenKeyPrefix + user.getId(), accessToken,
                jwtUtils.getExpiration(), TimeUnit.MILLISECONDS);
        redisTemplate.opsForValue().set(refreshTokenKeyPrefix + user.getId(), refreshToken,
                7, TimeUnit.DAYS);

        // 8. 组装返回信息
        Map<String, Object> result = new HashMap<>();
        result.put("token", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", 3); // 3 代表学员
        result.put("phone", phone);

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
     * 刷新 Token 逻辑
     */
    public String refreshToken(String refreshToken) {
        // 1. 验证 Refresh Token 格式与是否过期
        if (jwtUtils.isTokenExpired(refreshToken)) {
            throw new RuntimeException("Refresh Token 已过期，请重新登录");
        }

        // 2. 解析用户信息
        Long userId = jwtUtils.getUserId(refreshToken);
        String username = jwtUtils.getUsername(refreshToken);
        Integer role = jwtUtils.getRole(refreshToken);

        // 3. 校验 Redis 中的 Refresh Token 是否一致（防止重复利用或注销失效）
        String cachedRefreshToken = redisTemplate.opsForValue().get(refreshTokenKeyPrefix + userId);
        if (cachedRefreshToken == null || !cachedRefreshToken.equals(refreshToken)) {
            throw new RuntimeException("Refresh Token 无效或已在别处登录");
        }

        // 4. 生成新的 Access Token
        String newAccessToken = jwtUtils.generateToken(userId, username, role);

        // 5. 更新 Redis 中的 Access Token (可选，取决于注销策略)
        redisTemplate.opsForValue().set(tokenKeyPrefix + userId, newAccessToken,
                jwtUtils.getExpiration(), TimeUnit.MILLISECONDS);

        return newAccessToken;
    }

    /**
     * 检查 Token 在 Redis 中是否仍然有效（未被主动注销）
     */
    public boolean isTokenValid(Long userId, String token) {
        String redisKey = tokenKeyPrefix + userId;
        String cachedToken = redisTemplate.opsForValue().get(redisKey);
        return token.equals(cachedToken);
    }
}
