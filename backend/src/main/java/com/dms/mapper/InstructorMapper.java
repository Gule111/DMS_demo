package com.dms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.entity.Instructor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InstructorMapper extends BaseMapper<Instructor> {
    
    /**
     * 核心智能分配算法：查找匹配准教车型且当前负荷 (current_load) 最小的教练 ID
     * 如果有多个负荷相同的，取 id 最小的
     */
    @Select("SELECT id FROM biz_instructors WHERE teach_type = #{licenseType} ORDER BY current_load ASC, id ASC LIMIT 1")
    Long findBestInstructor(String licenseType);

    /**
     * 教练带教人数 +1
     */
    @Update("UPDATE biz_instructors SET current_load = current_load + 1 WHERE id = #{instructorId}")
    void incrementLoad(Long instructorId);
    
    /**
     * 教练带教人数 -1
     */
    @Update("UPDATE biz_instructors SET current_load = current_load - 1 WHERE id = #{instructorId} AND current_load > 0")
    void decrementLoad(Long instructorId);
}
