package com.dms.service;

import com.dms.common.QiniuUtil;
import com.dms.entity.Enrollment;
import com.dms.mapper.EnrollmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class EnrollmentService {

    private final EnrollmentMapper enrollmentMapper;
    private final QiniuUtil qiniuUtil;

    public EnrollmentService(EnrollmentMapper enrollmentMapper, QiniuUtil qiniuUtil) {
        this.enrollmentMapper = enrollmentMapper;
        this.qiniuUtil = qiniuUtil;
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitEnrollment(Long userId, String licenseType, 
                               MultipartFile idCardFront, 
                               MultipartFile idCardBack, 
                               MultipartFile healthCert) throws IOException {
        
        // 1. 获取业务学员 ID
        Long studentId = enrollmentMapper.getStudentIdByUserId(userId);
        if (studentId == null) {
            throw new RuntimeException("该用户不是学员，无法报名");
        }

        // 2. 更新报考类型和状态（状态1表示审核中）
        enrollmentMapper.updateStudentLicenseType(userId, licenseType);

        // 3. 上传文件到七牛云 OSS
        String frontUrl = qiniuUtil.uploadFile(idCardFront.getInputStream(), "uploads/id_cards/front/", idCardFront.getOriginalFilename());
        String backUrl = qiniuUtil.uploadFile(idCardBack.getInputStream(), "uploads/id_cards/back/", idCardBack.getOriginalFilename());
        String healthUrl = qiniuUtil.uploadFile(healthCert.getInputStream(), "uploads/health_certs/", healthCert.getOriginalFilename());

        // 4. 保存报名资料记录
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(studentId);
        enrollment.setIdCardFront(frontUrl);
        enrollment.setIdCardBack(backUrl);
        enrollment.setHealthCert(healthUrl);
        enrollment.setAuditStatus(0); // 0-待审核
        enrollmentMapper.insert(enrollment);

        // 5. 组装 JSON 并推送到 Redis 队列给 Python 审核
        // TODO: AI 审核逻辑，后续补充 Redis 推送代码
        System.out.println("成功保存报名记录，等待 AI 审核...");
    }
}
