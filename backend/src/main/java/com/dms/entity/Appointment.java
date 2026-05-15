package com.dms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("biz_appointments")
public class Appointment {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long studentId;
    
    private Long instructorId;
    
    private Date appointmentDate;
    
    private String timeSlot;
    
    private Integer subject; // 2-科目二, 3-科目三
    
    private Integer status; // 1-已预约, 2-已完成, 3-已取消
    
    private Date createdAt;
}
