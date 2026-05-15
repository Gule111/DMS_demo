package com.dms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.entity.TrainingRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TrainingRecordMapper extends BaseMapper<TrainingRecord> {
    
    @Select("SELECT * FROM biz_training_records WHERE student_id = #{studentId} ORDER BY training_date DESC")
    List<TrainingRecord> selectByStudentId(Long studentId);
}
