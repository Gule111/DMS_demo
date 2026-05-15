package com.dms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.entity.Exam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ExamMapper extends BaseMapper<Exam> {
    
    @Select("SELECT * FROM biz_exams WHERE student_id = #{studentId} ORDER BY exam_date DESC")
    List<Exam> selectByStudentId(Long studentId);
}
