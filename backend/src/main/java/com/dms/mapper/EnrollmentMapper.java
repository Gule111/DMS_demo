package com.dms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.entity.Enrollment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EnrollmentMapper extends BaseMapper<Enrollment> {
    
    /**
     * 根据 sys_users.id 查询对应的 biz_students.id
     */
    @Select("SELECT id FROM biz_students WHERE user_id = #{userId} LIMIT 1")
    Long getStudentIdByUserId(Long userId);
    
    /**
     * 根据 user_id 更新学员的报考类型
     */
    @Select("UPDATE biz_students SET license_type = #{licenseType}, status = 1 WHERE user_id = #{userId}")
    void updateStudentLicenseType(Long userId, String licenseType);

    /**
     * 获取学员最新的报名记录
     */
    @Select("SELECT * FROM biz_enrollments WHERE student_id = #{studentId} ORDER BY id DESC LIMIT 1")
    Enrollment getLatestEnrollment(Long studentId);

    /**
     * 管理员获取报名列表
     */
    @Select("<script>" +
            "SELECT e.id, e.student_id as studentId, e.id_card_front as idCardFront, " +
            "e.id_card_back as idCardBack, e.health_cert as healthCert, " +
            "e.audit_status as auditStatus, e.audit_remark as auditRemark, " +
            "e.auditor_id as auditorId, " +
            "s.real_name as student_name, s.phone as student_phone, s.license_type " +
            "FROM biz_enrollments e " +
            "JOIN biz_students s ON e.student_id = s.id " +
            "WHERE 1=1 " +
            "<if test='status != null'> AND e.audit_status = #{status} </if> " +
            "ORDER BY e.id DESC" +
            "</script>")
    java.util.List<java.util.Map<String, Object>> getAdminEnrollmentList(Integer status);
}
