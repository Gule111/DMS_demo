package com.dms.controller;

import com.dms.common.Result;
import com.dms.service.EnrollmentService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.dms.entity.Enrollment;

import java.util.Map;

@RestController
@RequestMapping("/enrollment")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    /**
     * 获取当前学员最新的报名状态
     */
    @GetMapping("/status")
    public Result<Enrollment> getStatus() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Result.success(enrollmentService.getLatestEnrollment(userId));
    }

    /**
     * 提交报名表单与材料
     */
    @PostMapping("/submit")
    public Result<String> submit(
            @RequestParam("licenseType") String licenseType,
            @RequestParam("idCardFront") MultipartFile idCardFront,
            @RequestParam("idCardBack") MultipartFile idCardBack,
            @RequestParam("healthCert") MultipartFile healthCert) {
        
        try {
            // 获取当前登录用户 ID
            Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            
            // 提交报名
            enrollmentService.submitEnrollment(userId, licenseType, idCardFront, idCardBack, healthCert);
            
            return Result.success("提交成功，系统正在进行智能排队审核");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("提交报名失败: " + e.getMessage());
        }
    }

    /**
     * 管理员：获取报名列表
     */
    @GetMapping("/admin/list")
    public Result<java.util.List<java.util.Map<String, Object>>> getAdminList(@RequestParam(value = "status", required = false) Integer status) {
        return Result.success(enrollmentService.getAdminEnrollmentList(status));
    }

    /**
     * 管理员：审核操作
     */
    @PostMapping("/admin/audit")
    public Result<Void> adminAudit(@RequestBody Map<String, Object> params) {
        Long enrollmentId = Long.valueOf(params.get("enrollmentId").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        String remark = (String) params.get("remark");
        Long auditorId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        enrollmentService.adminAudit(enrollmentId, status, remark, auditorId);
        return Result.success();
    }
}
