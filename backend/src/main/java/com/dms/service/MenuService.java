package com.dms.service;

import com.dms.entity.Menu;
import com.dms.mapper.MenuMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuService {

    private final MenuMapper menuMapper;

    public MenuService(MenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    /**
     * 获取角色的动态路由
     */
    public List<Menu> getRoutesByRole(Integer roleId) {
        // 后续可以在这里递归组装树形结构，目前先返回扁平列表，前端处理更灵活
        return menuMapper.selectMenusByRoleId(roleId);
    }
}
