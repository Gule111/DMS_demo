package com.dms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("biz_generated_documents")
public class GeneratedDocument {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long studentId; // 关联学员ID
    
    private String docType; // 文档类型，如 EnrollmentForm, HealthForm
    
    private String fileUrl; // 生成的PDF在七牛云上的URL
}
