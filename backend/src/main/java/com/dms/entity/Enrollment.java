package com.dms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("biz_enrollments")
public class Enrollment {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long studentId; // 关联 biz_students.id
    
    private String idCardFront; // 身份证正面URL
    
    private String idCardBack; // 身份证反面URL
    
    private String healthCert; // 体检证明URL
    
    private Integer auditStatus; // 审核状态: 0-待审核, 1-通过, 2-驳回
    
    private String auditRemark; // 审核意见
    
    private Long auditorId; // 审核人ID
}
