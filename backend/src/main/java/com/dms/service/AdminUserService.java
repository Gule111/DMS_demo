package com.dms.service;

import com.dms.dto.UserRoleDTO;
import com.dms.entity.User;
import com.dms.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUserService {

    private final UserMapper userMapper;
    private final AuthService authService;

    public AdminUserService(UserMapper userMapper, AuthService authService) {
        this.userMapper = userMapper;
        this.authService = authService;
    }

    public List<UserRoleDTO> getUserList() {
        return userMapper.getUserListWithRoles();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUserRole(Long userId, Long roleId) {
        // 1. 获取用户基本信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 删除旧角色映射
        userMapper.deleteUserRole(userId);
        
        // 3. 插入新角色映射 (roleId: 1-管理员, 2-教练员, 3-学员)
        userMapper.insertUserRole(userId, roleId);

        // 4. 业务约束：如果设置为教练员（roleId = 2），检查是否已有档案，没有则新建
        if (roleId == 2) {
            int exists = userMapper.checkBizInstructorExists(userId);
            if (exists == 0) {
                // 默认使用其 username 作为姓名，phone 作为电话
                String realName = user.getUsername();
                String phone = user.getPhone() != null ? user.getPhone() : user.getUsername();
                userMapper.insertBizInstructor(userId, realName, phone);
            }
        }

        // 5. 安全机制：强制重置该用户的 Token（包括 Access Token 和 Refresh Token）
        // 从 Redis 中清除对应的令牌 Key。这样该用户的旧会话和旧角色立即失效。
        // 下一次用户请求时，前端拦截器拦截到 401，并尝试使用已失效的 Refresh Token 刷新，最终迫使该用户强制下线。
        authService.logout(userId);
    }
}
