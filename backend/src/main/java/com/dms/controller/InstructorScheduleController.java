package com.dms.controller;

import com.dms.common.Result;
import com.dms.entity.InstructorSchedule;
import com.dms.service.InstructorScheduleService;
import com.dms.service.InstructorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/schedule")
public class InstructorScheduleController {

    private final InstructorScheduleService scheduleService;
    private final InstructorService instructorService;

    public InstructorScheduleController(InstructorScheduleService scheduleService, InstructorService instructorService) {
        this.scheduleService = scheduleService;
        this.instructorService = instructorService;
    }

    @GetMapping("/my")
    public Result<List<InstructorSchedule>> getMySchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Long instructorId = instructorService.getInstructorIdByUserId(userId);
            if (instructorId == null) return Result.error("非教练账号");
            return Result.success(scheduleService.getSchedule(instructorId, startDate, endDate));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/toggle")
    public Result<String> toggleBusy(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                   @RequestParam String timeSlot,
                                   @RequestParam boolean isBusy) {
        try {
            Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Long instructorId = instructorService.getInstructorIdByUserId(userId);
            if (instructorId == null) return Result.error("非教练账号");
            scheduleService.setBusy(instructorId, date, timeSlot, isBusy);
            return Result.success("更新成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
