package com.dms.controller;

import com.dms.common.Result;
import com.dms.entity.Exam;
import com.dms.service.ExamService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/exam")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    /**
     * 学员端：预约考试
     */
    @PostMapping("/book")
    public Result<String> bookExam(@RequestBody Map<String, Object> params) {
        try {
            Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Integer subject = (Integer) params.get("subject");
            String site = (String) params.get("examSite");
            // 简单处理日期转换，实际建议用更严谨的转换
            Long timestamp = Long.valueOf(params.get("examDate").toString());
            Date date = new Date(timestamp);
            
            examService.bookExam(userId, subject, date, site);
            return Result.success("预约申请已提交，请等待管理员审核");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 学员端：查看我的考试记录
     */
    @GetMapping("/my")
    public Result<List<Exam>> getMyExams() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Result.success(examService.getMyExams(userId));
    }

    /**
     * 管理员：获取申请列表
     */
    @GetMapping("/admin/list")
    public Result<List<Exam>> getAdminList(@RequestParam(required = false) Integer status) {
        return Result.success(examService.getAdminExamList(status));
    }

    /**
     * 管理员：审核预约
     */
    @PostMapping("/admin/audit")
    public Result<String> auditExam(@RequestBody Map<String, Object> params) {
        try {
            Long examId = Long.valueOf(params.get("id").toString());
            Integer status = (Integer) params.get("status");
            String site = (String) params.get("examSite");
            Date date = null;
            if (params.get("examDate") != null) {
                date = new Date(Long.valueOf(params.get("examDate").toString()));
            }
            
            examService.auditExam(examId, status, site, date);
            return Result.success("处理成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理员：录入成绩
     */
    @PostMapping("/admin/score")
    public Result<String> recordScore(@RequestBody Map<String, Object> params) {
        try {
            Long examId = Long.valueOf(params.get("id").toString());
            Integer score = (Integer) params.get("score");
            examService.recordScore(examId, score);
            return Result.success("成绩录入完毕，进度已更新");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
