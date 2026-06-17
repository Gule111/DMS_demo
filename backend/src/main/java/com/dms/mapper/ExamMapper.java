package com.dms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.entity.Exam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ExamMapper extends BaseMapper<Exam> {
    
    @Select("SELECT * FROM biz_exams WHERE student_id = #{studentId} ORDER BY exam_date DESC")
    List<Exam> selectByStudentId(Long studentId);

    @Select("<script>" +
            "SELECT e.*, s.real_name as student_name FROM biz_exams e " +
            "LEFT JOIN biz_students s ON e.student_id = s.id " +
            "<where>" +
            "  <if test='status != null'>e.status = #{status}</if>" +
            "</where>" +
            "ORDER BY e.exam_date DESC" +
            "</script>")
    List<Exam> selectAdminExamList(@Param("status") Integer status);
}
