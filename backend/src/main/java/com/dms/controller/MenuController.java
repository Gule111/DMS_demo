package com.dms.controller;

import com.dms.common.Result;
import com.dms.entity.Menu;
import com.dms.service.MenuService;
import com.dms.common.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/menu")
public class MenuController {

    private final MenuService menuService;
    private final JwtUtils jwtUtils;

    public MenuController(MenuService menuService, JwtUtils jwtUtils) {
        this.menuService = menuService;
        this.jwtUtils = jwtUtils;
    }

    /**
     * 获取当前用户的动态路由菜单
     */
    @GetMapping("/routes")
    public Result<List<Menu>> getMyRoutes(HttpServletRequest request) {
        try {
            // 从 Token 中解析出角色 ID
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error(401, "未获取到有效的认证信息");
            }
            
            String token = authHeader.substring(7);
            Integer roleId = jwtUtils.getRole(token);
            
            if (roleId == null) {
                return Result.error(401, "用户权限信息缺失");
            }
            
            List<Menu> menus = menuService.getRoutesByRole(roleId);
            return Result.success(menus);
        } catch (Exception e) {
            return Result.error(401, "身份验证失败: " + e.getMessage());
        }
    }
}
