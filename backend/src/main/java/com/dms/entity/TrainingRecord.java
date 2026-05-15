package com.dms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("biz_training_records")
public class TrainingRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long studentId;
    
    private Long instructorId;
    
    private Integer subject;
    
    private Date trainingDate;
    
    private BigDecimal hours;
    
    private String content;
    
    private Date createdAt;
}
