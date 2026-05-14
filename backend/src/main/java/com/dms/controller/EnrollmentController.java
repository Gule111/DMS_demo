package com.dms.controller;

import com.dms.common.Result;
import com.dms.service.EnrollmentService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/enrollment")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
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
}
