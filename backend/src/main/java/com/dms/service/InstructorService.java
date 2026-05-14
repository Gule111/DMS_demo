package com.dms.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.entity.Instructor;
import com.dms.entity.Student;
import com.dms.mapper.InstructorMapper;
import com.dms.mapper.StudentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InstructorService {

    private final InstructorMapper instructorMapper;
    private final StudentMapper studentMapper;

    public InstructorService(InstructorMapper instructorMapper, StudentMapper studentMapper) {
        this.instructorMapper = instructorMapper;
        this.studentMapper = studentMapper;
    }

    /**
     * 查询所有教练列表 (供管理员和学员查看)
     */
    public List<Instructor> getAllInstructors() {
        return instructorMapper.selectList(new QueryWrapper<Instructor>().orderByAsc("id"));
    }

    /**
     * 新增教练
     */
    public void addInstructor(Instructor instructor) {
        instructor.setCurrentLoad(0); // 初始负荷为 0
        instructorMapper.insert(instructor);
    }

    /**
     * 修改教练信息
     */
    public void updateInstructor(Instructor instructor) {
        instructorMapper.updateById(instructor);
    }

    /**
     * 删除教练
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteInstructor(Long id) {
        Instructor instructor = instructorMapper.selectById(id);
        if (instructor != null) {
            // 如果有带教学员，先将学员解绑回退到待分配状态
            if (instructor.getCurrentLoad() > 0) {
                studentMapper.unbindStudentsByInstructor(id);
            }
            instructorMapper.deleteById(id);
        }
    }

    /**
     * 查看教练名下的所有学员
     */
    public List<Student> getStudentsByInstructor(Long instructorId) {
        return studentMapper.selectList(new QueryWrapper<Student>().eq("instructor_id", instructorId));
    }

    /**
     * 获取最匹配的空闲教练 (仅查询，不绑定)
     */
    public Instructor getBestInstructor(String licenseType) {
        Long bestInstructorId = instructorMapper.findBestInstructor(licenseType);
        if (bestInstructorId == null) {
            throw new RuntimeException("当前没有匹配 " + licenseType + " 车型的教练");
        }
        return instructorMapper.selectById(bestInstructorId);
    }

    /**
     * 自动为学员分配教练 (智能负载均衡算法)
     * @param studentId 学员ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void autoAssignInstructor(Long studentId) {
        // 1. 获取学员信息和报考车型
        Student student = studentMapper.selectById(studentId);
        if (student == null || student.getLicenseType() == null) {
            throw new RuntimeException("学员信息不完整，无法分配教练");
        }

        // 2. 核心算法：寻找匹配车型且当前负荷最低的教练
        Long bestInstructorId = instructorMapper.findBestInstructor(student.getLicenseType());
        if (bestInstructorId == null) {
            throw new RuntimeException("当前没有匹配 " + student.getLicenseType() + " 车型的教练可供分配");
        }

        // 3. 绑定关系并更新学员状态
        doAssign(student, bestInstructorId);
    }

    /**
     * 手动为学员指定教练
     * @param studentId 学员ID
     * @param instructorId 教练ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void manualAssignInstructor(Long studentId, Long instructorId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new RuntimeException("学员不存在");
        }
        
        Instructor instructor = instructorMapper.selectById(instructorId);
        if (instructor == null) {
            throw new RuntimeException("指定的教练不存在");
        }
        
        // 校验车型是否匹配
        if (!instructor.getTeachType().equals(student.getLicenseType())) {
            throw new RuntimeException("该教练不支持学员报考的车型 (" + student.getLicenseType() + ")");
        }

        doAssign(student, instructorId);
    }

    /**
     * 执行绑定和负荷更新的内部事务逻辑
     */
    private void doAssign(Student student, Long newInstructorId) {
        // 如果学员原来已经有教练了，老教练的负荷要 -1
        if (student.getInstructorId() != null) {
            instructorMapper.decrementLoad(student.getInstructorId());
        }

        // 绑定新教练
        studentMapper.assignInstructor(student.getId(), newInstructorId);

        // 新教练的负荷 +1
        instructorMapper.incrementLoad(newInstructorId);
    }
}
