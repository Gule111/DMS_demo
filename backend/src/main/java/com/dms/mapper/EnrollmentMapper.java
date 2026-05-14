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
}
