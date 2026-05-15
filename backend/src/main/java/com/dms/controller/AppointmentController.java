package com.dms.controller;

import com.dms.common.Result;
import com.dms.entity.Appointment;
import com.dms.service.AppointmentService;
import com.dms.service.InstructorService;
import jakarta.annotation.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/appointment")
public class AppointmentController {

    @Resource
    private InstructorService instructorService;
    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping("/book")
    public Result<String> book(@RequestBody Appointment appointment) {
        try {
            Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            appointmentService.bookSlot(userId, appointment);
            return Result.success("预约成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/my")
    public Result<List<Appointment>> getMy() {
        try {
            Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return Result.success(appointmentService.getMyAppointments(userId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/cancel/{id}")
    public Result<String> cancel(@PathVariable("id") Long id) {
        try {
            appointmentService.cancelAppointment(id);
            return Result.success("预约已取消");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 教练端：获取预约列表
     */
    @GetMapping("/instructor/list")
    public Result<List<Appointment>> getInstructorList() {
        try {
            Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Long instructorId = instructorService.getInstructorIdByUserId(userId);
            return Result.success(appointmentService.getInstructorAppointments(instructorId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 教练端：处理预约 (接受/拒绝)
     */
    @PostMapping("/handle")
    public Result<String> handle(@RequestParam("id") Long id, @RequestParam("status") Integer status) {
        try {
            appointmentService.handleAppointment(id, status);
            return Result.success("处理成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 教练端：确认完成练车
     */
    @PostMapping("/complete/{id}")
    public Result<String> complete(@PathVariable("id") Long id, @RequestParam(value = "feedback", required = false) String feedback) {
        try {
            appointmentService.completeAppointment(id, feedback);
            return Result.success("确认练车已完成");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
