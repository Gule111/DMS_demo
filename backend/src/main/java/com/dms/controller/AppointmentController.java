package com.dms.controller;

import com.dms.common.Result;
import com.dms.entity.Appointment;
import com.dms.service.AppointmentService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/appointment")
public class AppointmentController {

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
