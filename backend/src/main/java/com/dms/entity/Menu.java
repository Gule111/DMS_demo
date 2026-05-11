package com.dms.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("sys_menus")
public class Menu {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String menuName;
    private String path;
    private String component;
    private String perms;
    private String menuType; // M-目录, C-菜单, F-按钮
    private String icon;
    private Integer sortOrder;
    private Integer status;
}
