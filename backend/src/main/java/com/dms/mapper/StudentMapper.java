package com.dms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dms.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {

    /**
     * 为学员绑定教练，并将状态更新为 2 (学习中)
     */
    @Update("UPDATE biz_students SET instructor_id = #{instructorId}, status = 2 WHERE id = #{studentId}")
    void assignInstructor(Long studentId, Long instructorId);

    /**
     * 教练被删除时，将其名下的学员解绑回退到待分配状态 (1)
     */
    @Update("UPDATE biz_students SET instructor_id = NULL, status = 1 WHERE instructor_id = #{instructorId}")
    void unbindStudentsByInstructor(Long instructorId);

    @Update("UPDATE biz_students SET instructor_id = #{instructorId} WHERE id = #{studentId}")
    void bindInstructor(Long studentId, Long instructorId);

    @Update("UPDATE biz_students SET status = #{status} WHERE id = #{id}")
    void updateStatus(Long id, Integer status);
}
