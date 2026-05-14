package com.dms.controller;

import com.dms.common.Result;
import com.dms.entity.GeneratedDocument;
import com.dms.service.PdfService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/document")
public class DocumentController {

    private final PdfService pdfService;
    private final com.dms.mapper.EnrollmentMapper enrollmentMapper;

    public DocumentController(PdfService pdfService, com.dms.mapper.EnrollmentMapper enrollmentMapper) {
        this.pdfService = pdfService;
        this.enrollmentMapper = enrollmentMapper;
    }

    /**
     * 手动触发生成学员的报名表 PDF
     */
    @PostMapping("/generate/enrollment")
    public Result<GeneratedDocument> generateEnrollmentForm() {
        try {
            Long userId = (Long) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Long studentId = enrollmentMapper.getStudentIdByUserId(userId);
            if (studentId == null) {
                return Result.error("您不是学员，无法生成表单");
            }
            
            GeneratedDocument document = pdfService.generateEnrollmentForm(studentId);
            return Result.success(document);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("文档生成失败: " + e.getMessage());
        }
    }
}
