package com.dms.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.entity.Appointment;
import com.dms.entity.Student;
import com.dms.mapper.AppointmentMapper;
import com.dms.mapper.StudentMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentMapper appointmentMapper;
    private final StudentMapper studentMapper;
    private final ProgressService progressService;

    public AppointmentService(AppointmentMapper appointmentMapper, StudentMapper studentMapper, ProgressService progressService) {
        this.appointmentMapper = appointmentMapper;
        this.studentMapper = studentMapper;
        this.progressService = progressService;
    }

    public void bookSlot(Long userId, Appointment appointment) {
        Student student = studentMapper.selectOne(new QueryWrapper<Student>().eq("user_id", userId));
        if (student == null || student.getInstructorId() == null) {
            throw new RuntimeException("尚未分配教练，无法预约");
        }
        
        appointment.setStudentId(student.getId());
        appointment.setInstructorId(student.getInstructorId());
        appointment.setStatus(1); // 已预约
        appointmentMapper.insert(appointment);
    }

    public List<Appointment> getMyAppointments(Long userId) {
        Student student = studentMapper.selectOne(new QueryWrapper<Student>().eq("user_id", userId));
        if (student == null) return List.of();
        return appointmentMapper.getStudentAppointments(student.getId());
    }

    /**
     * 教练端：获取我的待处理预约
     */
    public List<Appointment> getInstructorAppointments(Long instructorId) {
        return appointmentMapper.selectList(new QueryWrapper<Appointment>()
                .eq("instructor_id", instructorId)
                .orderByDesc("appointment_date"));
    }

    /**
     * 教练端：处理预约 (接受/拒绝)
     * status: 2-已接受, 3-已拒绝
     */
    public void handleAppointment(Long appointmentId, Integer status) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) throw new RuntimeException("记录不存在");
        appointment.setStatus(status);
        appointmentMapper.updateById(appointment);
    }

    public void cancelAppointment(Long id) {
        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment != null) {
            appointment.setStatus(3); // 已取消
            appointmentMapper.updateById(appointment);
        }
    }

    /**
     * 教练确认练车完成
     */
    public void completeAppointment(Long appointmentId, String feedback) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null || appointment.getStatus() != 2) {
            throw new RuntimeException("预约记录状态不正确（仅限已通过的预约）");
        }

        // 1. 更新预约状态为已完成 (status = 2)
        appointment.setStatus(2);
        appointmentMapper.updateById(appointment);

        // 2. 自动产生训练记录并更新学时 (假设一节课 2 小时)
        progressService.recordTraining(
            appointment.getInstructorId(), 
            appointment.getStudentId(), 
            appointment.getSubject(), 
            new java.math.BigDecimal("2.0"), 
            feedback
        );
    }
}
