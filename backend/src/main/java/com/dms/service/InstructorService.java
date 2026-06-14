package com.dms.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.entity.Instructor;
import com.dms.entity.Student;
import com.dms.mapper.InstructorMapper;
import com.dms.mapper.StudentMapper;
import com.dms.mapper.UserMapper;
import com.dms.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InstructorService {

    private final InstructorMapper instructorMapper;
    private final StudentMapper studentMapper;
    private final UserMapper userMapper;

    public InstructorService(InstructorMapper instructorMapper, StudentMapper studentMapper, UserMapper userMapper) {
        this.instructorMapper = instructorMapper;
        this.studentMapper = studentMapper;
        this.userMapper = userMapper;
    }

    /**
     * 查询所有教练列表 (供管理员和学员查看)
     */
    public List<Instructor> getAllInstructors() {
        return instructorMapper.selectList(new QueryWrapper<Instructor>().orderByAsc("id"));
    }

    /**
     * 根据用户ID获取教练ID
     * 如果记录缺失，则自动尝试创建（容错逻辑）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long getInstructorIdByUserId(Long userId) {
        Instructor instructor = instructorMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Instructor>()
                .eq(Instructor::getUserId, userId)
        );
        
        if (instructor != null) {
            return instructor.getId();
        }

        // 容错逻辑：如果没找到但角色是教练，则自动创建一个默认记录
        try {
            Instructor newInstructor = new Instructor();
            newInstructor.setUserId(userId);
            newInstructor.setRealName("系统分配教练");
            newInstructor.setPhone("13800000000");
            newInstructor.setTeachType("C1");
            newInstructor.setCurrentLoad(0);
            instructorMapper.insert(newInstructor);
            return newInstructor.getId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 新增教练
     */
    @Transactional(rollbackFor = Exception.class)
    public void addInstructor(Instructor instructor) {
        // 1. 创建对应的 sys_users 账号
        User user = new User();
        user.setUsername(instructor.getPhone()); // 默认手机号作为登录名
        user.setPhone(instructor.getPhone());
        user.setPassword(cn.hutool.crypto.digest.DigestUtil.md5Hex("Aq123456"));
        user.setStatus(1);
        user.setCreatedAt(java.time.LocalDateTime.now());
        
        // 检查用户是否已存在 (根据手机号)
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User> query = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        query.eq(User::getPhone, instructor.getPhone()).or().eq(User::getUsername, instructor.getPhone());
        User existUser = userMapper.selectOne(query);
        if (existUser != null) {
            throw new RuntimeException("该手机号已被注册，无法新增为新教练");
        }
        
        userMapper.insert(user);

        // 2. 分配教练角色 (role_id = 2)
        userMapper.insertUserRole(user.getId(), 2L);

        // 3. 关联用户 ID 并插入教练表
        instructor.setUserId(user.getId());
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
            // 1. 获取名下所有学员
            List<Student> students = getStudentsByInstructor(id);

            // 2. 如果有带教学员，先将学员解绑回退到待分配状态
            if (instructor.getCurrentLoad() > 0) {
                studentMapper.unbindStudentsByInstructor(id);
            }
            
            // 3. 删除教练表记录
            instructorMapper.deleteById(id);

            // 4. 删除 sys_users 相关账号及角色信息
            if (instructor.getUserId() != null) {
                userMapper.deleteUserRole(instructor.getUserId());
                userMapper.deleteById(instructor.getUserId());
            }

            // 5. 尝试为被解绑的学员自动重新分配教练
            for (Student student : students) {
                try {
                    // 调用已有的自动分配算法
                    autoAssignInstructor(student.getId());
                } catch (Exception e) {
                    // 如果分配失败（例如当前没有空闲匹配教练），不阻断删除流程，保留待分配状态
                    System.err.println("自动重新分配教练失败 (学员ID: " + student.getId() + "): " + e.getMessage());
                }
            }
        }
    }

    /**
     * 查看教练名下的所有学员
     */
    public List<Student> getStudentsByInstructor(Long instructorId) {
        return studentMapper.selectList(new QueryWrapper<Student>().eq("instructor_id", instructorId));
    }

    /**
     * 根据学员关联ID获取教练
     */
    public Instructor getInstructorByStudentUserId(Long studentUserId) {
        Student student = studentMapper.selectOne(new QueryWrapper<Student>().eq("user_id", studentUserId));
        if (student == null || student.getInstructorId() == null) {
            return null;
        }
        return instructorMapper.selectById(student.getInstructorId());
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
