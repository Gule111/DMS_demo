package com.dms.controller;

import com.dms.common.Result;
import com.dms.entity.GeneratedDocument;
import com.dms.mapper.GeneratedDocumentMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/docs")
public class GeneratedDocumentController {

    private final GeneratedDocumentMapper documentMapper;

    public GeneratedDocumentController(GeneratedDocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    /**
     * 获取当前登录学员名下的所有已生成的电子文档
     */
    @GetMapping("/my")
    public Result<List<GeneratedDocument>> getMyDocuments() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long studentId = documentMapper.getStudentIdByUserId(userId);
        
        if (studentId == null) {
            return Result.success(List.of()); // 非学员或未报名
        }

        List<GeneratedDocument> docs = documentMapper.getDocumentsByStudentId(studentId);
        
        // 由于是按照 id DESC 排序，所以相同类型的文档，最先遇到的一定是最新的
        List<GeneratedDocument> latestDocs = new java.util.ArrayList<>();
        java.util.Set<String> seenTypes = new java.util.HashSet<>();
        
        for (GeneratedDocument doc : docs) {
            if (!seenTypes.contains(doc.getDocType())) {
                latestDocs.add(doc);
                seenTypes.add(doc.getDocType());
            }
        }
        
        return Result.success(latestDocs);
    }
}
