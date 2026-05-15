package com.dms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.entity.GeneratedDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface GeneratedDocumentMapper extends BaseMapper<GeneratedDocument> {
    
    /**
     * 获取学员的基础信息用于生成表单
     */
    @Select("SELECT s.id, s.real_name, s.id_card, s.phone, s.license_type " +
            "FROM biz_students s WHERE s.id = #{studentId}")
    Map<String, Object> getStudentInfoForPdf(Long studentId);

    @Select("SELECT id FROM biz_students WHERE user_id = #{userId}")
    Long getStudentIdByUserId(Long userId);

    @Select("SELECT * FROM biz_generated_documents WHERE student_id = #{studentId} ORDER BY id DESC")
    java.util.List<GeneratedDocument> getDocumentsByStudentId(Long studentId);
}
