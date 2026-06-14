package com.dms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.common.Result;
import com.dms.entity.Student;
import com.dms.mapper.StudentMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/student")
public class StudentController {

    private final StudentMapper studentMapper;

    public StudentController(StudentMapper studentMapper) {
        this.studentMapper = studentMapper;
    }

    /**
     * 获取待分配教练的学员列表 (通过且未分配教练)
     */
    @GetMapping("/pending")
    public Result<List<Student>> getPendingStudents() {
        return Result.success(studentMapper.getPendingStudents());
    }

    /**
     * 获取已分配教练的学员列表
     */
    @GetMapping("/assigned")
    public Result<List<Student>> getAssignedStudents() {
        QueryWrapper<Student> query = new QueryWrapper<>();
        // 已绑定教练
        query.isNotNull("instructor_id");
        return Result.success(studentMapper.selectList(query));
    }
}
