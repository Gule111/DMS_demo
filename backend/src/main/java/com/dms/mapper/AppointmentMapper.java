package com.dms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.entity.Appointment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {
    
    @Select("SELECT * FROM biz_appointments WHERE student_id = #{studentId} ORDER BY appointment_date DESC, time_slot DESC")
    List<Appointment> getStudentAppointments(Long studentId);
}
