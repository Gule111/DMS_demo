package com.dms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("biz_learning_progress")
public class LearningProgress {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long studentId;
    
    private Integer subject; // 1, 2, 3, 4
    
    private Integer hoursDone;
    
    private Integer status; // 0-进行中, 1-已达标/待考, 2-已通过
}
