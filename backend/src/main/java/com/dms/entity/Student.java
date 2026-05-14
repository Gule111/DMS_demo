package com.dms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("biz_students")
public class Student {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String realName;
    
    private String idCard;
    
    private String phone;
    
    private String licenseType;
    
    private Long instructorId; // 分配的教练ID
    
    private String instructorReq; // 对教练的要求
    
    private Integer status; // 0-未报名, 1-审核中, 2-学习中, 3-已拿证
}
