package com.dms.controller;

import com.dms.common.Result;
import com.dms.entity.LearningProgress;
import com.dms.entity.TrainingRecord;
import com.dms.service.ProgressService;
import com.dms.service.InstructorService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/progress")
public class ProgressController {

    private final ProgressService progressService;
    private final InstructorService instructorService;

    public ProgressController(ProgressService progressService, InstructorService instructorService) {
        this.progressService = progressService;
        this.instructorService = instructorService;
    }

    /**
     * 学员端：获取自己的学习进度
     */
    @GetMapping("/my")
    public Result<List<com.dms.dto.LearningProgressDTO>> getMyProgress() {
        try {
            Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return Result.success(progressService.getStudentProgress(userId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 学员端：获取自己的练车记录
     */
    @GetMapping("/records")
    public Result<List<TrainingRecord>> getMyRecords() {
        try {
            Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return Result.success(progressService.getMyRecords(userId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 教练端：手动录入学时
     */
    @PostMapping("/record")
    public Result<String> recordTraining(@RequestParam Long studentId, 
                                       @RequestParam Integer subject, 
                                       @RequestParam BigDecimal hours, 
                                       @RequestParam String content) {
        try {
            Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Long instructorId = instructorService.getInstructorIdByUserId(userId);
            if (instructorId == null) {
                return Result.error("当前用户不是教练，无法录入学时");
            }
            progressService.recordTraining(instructorId, studentId, subject, hours, content);
            return Result.success("学时录入成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 教练端：录入考试成绩/评价
     */
    @PostMapping("/exam-result")
    public Result<String> recordExamResult(@RequestParam Long studentId,
                                         @RequestParam Integer subject,
                                         @RequestParam Integer score,
                                         @RequestParam(required = false) String remark) {
        try {
            Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Long instructorId = instructorService.getInstructorIdByUserId(userId);
            if (instructorId == null) {
                return Result.error("当前用户不是教练，无法操作");
            }
            progressService.recordExamResult(studentId, subject, score, remark);
            return Result.success("成绩录入成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
