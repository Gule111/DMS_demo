package com.dms.controller;

import com.dms.common.Result;
import com.dms.dto.UserRoleDTO;
import com.dms.service.AdminUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')") // 只有管理员可以访问
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * 获取系统所有用户及其角色信息
     */
    @GetMapping
    public Result<List<UserRoleDTO>> getUserList() {
        return Result.success(adminUserService.getUserList());
    }

    /**
     * 更新用户角色
     */
    @PutMapping("/{userId}/role")
    public Result<String> updateUserRole(@PathVariable Long userId, @RequestBody Map<String, Long> payload) {
        Long roleId = payload.get("roleId");
        if (roleId == null) {
            return Result.error("角色 ID 不能为空");
        }
        try {
            adminUserService.updateUserRole(userId, roleId);
            return Result.success("角色修改成功");
        } catch (Exception e) {
            return Result.error("角色修改失败：" + e.getMessage());
        }
    }
}
