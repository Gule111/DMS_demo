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

        // 4. 生成 JWT Token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), roleId);

        // 5. 将 Token 存入 Redis（用于主动注销和续期管理）
        String redisKey = tokenKeyPrefix + user.getId();
        redisTemplate.opsForValue().set(redisKey, token,
                jwtUtils.getExpiration(), TimeUnit.MILLISECONDS);

        // 6. 组装返回信息
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", roleId); // 返回数字 ID: 1, 2, 3
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

        // 6. 注册成功后自动登录，生成 Token (3为学员角色ID)
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), 3);

        // 7. Token 存入 Redis
        String redisKey = tokenKeyPrefix + user.getId();
        redisTemplate.opsForValue().set(redisKey, token,
                jwtUtils.getExpiration(), TimeUnit.MILLISECONDS);

        // 8. 组装返回信息
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
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
