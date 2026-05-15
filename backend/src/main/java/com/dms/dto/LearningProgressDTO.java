package com.dms.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class LearningProgressDTO {
    private Long id;
    private Long studentId;
    private Integer subject;
    private Integer hoursDone;
    private Integer status; // 0-进行中, 1-学时达标, 2-已通过
    private Integer latestScore; // 最新考试成绩
}
