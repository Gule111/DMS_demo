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

    public AdminUserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public List<UserRoleDTO> getUserList() {
        return userMapper.getUserListWithRoles();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUserRole(Long userId, Long roleId) {
        // 获取用户基本信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 删除旧角色
        userMapper.deleteUserRole(userId);
        
        // 插入新角色
        userMapper.insertUserRole(userId, roleId);

        // 业务约束：如果设置为教练员（roleId = 2），检查是否已有档案，没有则新建
        if (roleId == 2) {
            int exists = userMapper.checkBizInstructorExists(userId);
            if (exists == 0) {
                // 默认使用其 username 作为姓名，phone 作为电话
                String realName = user.getUsername();
                String phone = user.getPhone() != null ? user.getPhone() : user.getUsername();
                userMapper.insertBizInstructor(userId, realName, phone);
            }
        }
    }
}
