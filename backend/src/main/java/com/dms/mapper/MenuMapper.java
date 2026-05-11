package com.dms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.entity.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MenuMapper extends BaseMapper<Menu> {
    
    /**
     * 根据角色ID查询授权菜单
     */
    @Select("SELECT m.* FROM sys_menus m " +
            "INNER JOIN sys_role_menus rm ON m.id = rm.menu_id " +
            "WHERE rm.role_id = #{roleId} AND m.status = 1 " +
            "ORDER BY m.sort_order ASC")
    List<Menu> selectMenusByRoleId(Integer roleId);
}
