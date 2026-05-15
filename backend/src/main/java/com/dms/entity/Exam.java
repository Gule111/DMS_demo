package com.dms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("biz_exams")
public class Exam {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long studentId;
    
    private Integer subject; // 考试科目 1, 2, 3, 4
    
    private Date examDate; // 考试日期
    
    private String examSite; // 考试地点
    
    private Integer status; // 状态: 0-待审核, 1-预约成功, 2-考试完成
    
    private Integer score; // 考试成绩
}
