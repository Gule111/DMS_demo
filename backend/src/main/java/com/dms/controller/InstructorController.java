package com.dms.controller;

import com.dms.common.Result;
import com.dms.entity.Instructor;
import com.dms.entity.Student;
import com.dms.service.InstructorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/instructor")
public class InstructorController {

    private final InstructorService instructorService;

    public InstructorController(InstructorService instructorService) {
        this.instructorService = instructorService;
    }

    /**
     * 获取所有教练列表
     */
    @GetMapping("/list")
    public Result<List<Instructor>> listInstructors() {
        return Result.success(instructorService.getAllInstructors());
    }

    @PostMapping("/add")
    public Result<String> addInstructor(@RequestBody Instructor instructor) {
        instructorService.addInstructor(instructor);
        return Result.success("新增教练成功");
    }

    @PutMapping("/update")
    public Result<String> updateInstructor(@RequestBody Instructor instructor) {
        instructorService.updateInstructor(instructor);
        return Result.success("修改教练信息成功");
    }

    @DeleteMapping("/delete/{id}")
    public Result<String> deleteInstructor(@PathVariable("id") Long id) {
        try {
            instructorService.deleteInstructor(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/students/{id}")
    public Result<List<Student>> getStudentsByInstructor(@PathVariable("id") Long id) {
        return Result.success(instructorService.getStudentsByInstructor(id));
    }

    /**
     * 手动分配教练接口
     */
    @PostMapping("/assign/manual")
    public Result<String> manualAssign(@RequestParam("studentId") Long studentId, 
                                     @RequestParam("instructorId") Long instructorId) {
        try {
            instructorService.manualAssignInstructor(studentId, instructorId);
            return Result.success("手动分配教练成功！");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 触发自动分配教练接口 (实际业务中该方法可由后台审核通过事件自动触发)
     */
    @PostMapping("/assign/auto")
    public Result<String> autoAssign(@RequestParam("studentId") Long studentId) {
        try {
            instructorService.autoAssignInstructor(studentId);
            return Result.success("智能分配教练成功！");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取最匹配的教练（仅查询，用于弹窗预览）
     */
    @GetMapping("/best-match")
    public Result<Instructor> getBestMatch(@RequestParam("licenseType") String licenseType) {
        try {
            return Result.success(instructorService.getBestInstructor(licenseType));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
