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
     * 获取待分配教练的学员列表 (状态为 1-审核中/通过未分配)
     */
    @GetMapping("/pending")
    public Result<List<Student>> getPendingStudents() {
        QueryWrapper<Student> query = new QueryWrapper<>();
        // 状态 1 且未绑定教练
        query.eq("status", 1).isNull("instructor_id");
        return Result.success(studentMapper.selectList(query));
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
