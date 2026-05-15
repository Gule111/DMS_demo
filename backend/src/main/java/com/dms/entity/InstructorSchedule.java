package com.dms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("biz_instructor_schedule")
public class InstructorSchedule {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long instructorId;
    
    private LocalDate workDate;
    
    private String timeSlot;
    
    private Integer isBusy; // 0-空闲, 1-忙碌
}
