package com.dms.service;

import com.dms.common.QiniuUtil;
import com.dms.entity.Enrollment;
import com.dms.mapper.EnrollmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.google.gson.Gson;
import com.dms.mapper.InstructorMapper;
import com.dms.mapper.StudentMapper;

@Service
public class EnrollmentService {

    private final EnrollmentMapper enrollmentMapper;
    private final QiniuUtil qiniuUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final Gson gson;
    private final PdfService pdfService;
    private final InstructorMapper instructorMapper;
    private final StudentMapper studentMapper;
    private final ProgressService progressService;

    public EnrollmentService(EnrollmentMapper enrollmentMapper, QiniuUtil qiniuUtil, StringRedisTemplate stringRedisTemplate, 
                             PdfService pdfService, InstructorMapper instructorMapper, StudentMapper studentMapper,
                             ProgressService progressService) {
        this.enrollmentMapper = enrollmentMapper;
        this.qiniuUtil = qiniuUtil;
        this.stringRedisTemplate = stringRedisTemplate;
        this.pdfService = pdfService;
        this.instructorMapper = instructorMapper;
        this.studentMapper = studentMapper;
        this.progressService = progressService;
        this.gson = new Gson();
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
        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("enrollmentId", enrollment.getId());
        taskMap.put("studentId", studentId);
        taskMap.put("idCardFront", frontUrl);
        taskMap.put("idCardBack", backUrl);
        taskMap.put("healthCert", healthUrl);
        taskMap.put("licenseType", licenseType);

        String jsonStr = gson.toJson(taskMap);
        stringRedisTemplate.opsForList().leftPush("dms:enrollment:audit:queue", jsonStr);
        System.out.println("成功保存报名记录，推送到 Redis 队列: " + jsonStr);
    }

    /**
     * 查询学员最新的报名状态
     */
    public Enrollment getLatestEnrollment(Long userId) {
        Long studentId = enrollmentMapper.getStudentIdByUserId(userId);
        if (studentId == null) return null;
        return enrollmentMapper.getLatestEnrollment(studentId);
    }

    /**
     * 管理员查询报名列表
     */
    public java.util.List<java.util.Map<String, Object>> getAdminEnrollmentList(Integer status) {
        return enrollmentMapper.getAdminEnrollmentList(status);
    }

    /**
     * 管理员审核操作
     */
    public void adminAudit(Long enrollmentId, Integer status, String remark, Long auditorId) {
        Enrollment enrollment = enrollmentMapper.selectById(enrollmentId);
        if (enrollment == null) throw new RuntimeException("报名记录不存在");

        enrollment.setAuditStatus(status);
        enrollment.setAuditRemark(remark);
        enrollment.setAuditorId(auditorId);
        enrollmentMapper.updateById(enrollment);

        // 如果终审通过(Status 3)，则需要更新学员表的状态并生成电子档案
        if (status == 3) {
            System.out.println("报名终审通过，正在执行后续逻辑，学员ID: " + enrollment.getStudentId());
            
            // 1. 自动分配教练
            // 先获取报名信息（主要是车型）
            Enrollment fullEnrollment = enrollmentMapper.selectById(enrollmentId);
            String licenseType = studentMapper.selectById(fullEnrollment.getStudentId()).getLicenseType();
            Long bestInstructorId = instructorMapper.findBestInstructor(licenseType);
            
            if (bestInstructorId != null) {
                // 绑定教练
                studentMapper.bindInstructor(fullEnrollment.getStudentId(), bestInstructorId);
                // 教练负荷 +1
                instructorMapper.incrementLoad(bestInstructorId);
                System.out.println("已为学员分配最佳教练ID: " + bestInstructorId);
            }
            
            // 2. 更新学员状态为“学习中” (status = 2)
            studentMapper.updateStatus(fullEnrollment.getStudentId(), 2);

            // 3. 初始化学习进度 (科目一到科目四)
            progressService.initProgress(fullEnrollment.getStudentId());

            // 4. 触发异步 PDF 生成任务
            pdfService.generateAllPdfsAsync(enrollment.getStudentId());
        }
    }
}
