package com.dms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("biz_instructors")
public class Instructor {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId; // 关联sys_users.id
    
    private String realName; // 教练姓名
    
    private String phone; // 联系电话
    
    private String teachType; // 准教车型 (例如: C1, C2)
    
    private Integer currentLoad; // 当前带教人数
}
