package com.dms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.entity.LearningProgress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface LearningProgressMapper extends BaseMapper<LearningProgress> {
    
    @Select("SELECT * FROM biz_learning_progress WHERE student_id = #{studentId} ORDER BY subject ASC")
    List<LearningProgress> selectByStudentId(Long studentId);
    
    @Update("UPDATE biz_learning_progress SET hours_done = hours_done + #{hours} WHERE student_id = #{studentId} AND subject = #{subject}")
    int addHours(Long studentId, Integer subject, Integer hours);
}
