package com.dms.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.entity.Exam;
import com.dms.entity.LearningProgress;
import com.dms.entity.Student;
import com.dms.mapper.ExamMapper;
import com.dms.mapper.LearningProgressMapper;
import com.dms.mapper.StudentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class ExamService {

    private final ExamMapper examMapper;
    private final StudentMapper studentMapper;
    private final LearningProgressMapper progressMapper;
    private final ProgressService progressService;

    public ExamService(ExamMapper examMapper, 
                       StudentMapper studentMapper, 
                       LearningProgressMapper progressMapper,
                       ProgressService progressService) {
        this.examMapper = examMapper;
        this.studentMapper = studentMapper;
        this.progressMapper = progressMapper;
        this.progressService = progressService;
    }

    /**
     * 学员预约考试
     */
    @Transactional(rollbackFor = Exception.class)
    public void bookExam(Long userId, Integer subject, Date examDate, String examSite, Integer examType) {
        if (examType == null) examType = 1;
        Student student = studentMapper.selectOne(new QueryWrapper<Student>().eq("user_id", userId));
        if (student == null) throw new RuntimeException("学员信息不存在");

        // 校验是否已经通过该科目的正式考试
        LearningProgress progress = progressMapper.selectOne(
            new QueryWrapper<LearningProgress>().eq("student_id", student.getId()).eq("subject", subject)
        );
        if (progress != null && progress.getStatus() == 2) {
            throw new RuntimeException("您已通过该科目的正式考试，无需再次预约");
        }

        // 校验是否跳级预约
        if (subject > 1) {
            LearningProgress prevProgress = progressMapper.selectOne(
                new QueryWrapper<LearningProgress>().eq("student_id", student.getId()).eq("subject", subject - 1)
            );
            if (prevProgress == null || prevProgress.getStatus() != 2) {
                throw new RuntimeException("请先通过科目" + (subject - 1) + "的正式考试，再预约科目" + subject);
            }
        }

        // 校验学时是否达标 (科目一、科目二和科目三均有学时要求)
        if (subject == 1 || subject == 2 || subject == 3) {
            if (progress == null || progress.getStatus() == 0) {
                throw new RuntimeException("学时未达标，无法预约考试");
            }
        }

        // 校验是否已经有待参加的同科目考试
        Exam existing = examMapper.selectOne(new QueryWrapper<Exam>()
            .eq("student_id", student.getId())
            .eq("subject", subject)
            .eq("exam_type", examType)
            .in("status", 0, 1)); // 0-待审核, 1-预约成功
        if (existing != null) {
            throw new RuntimeException("您已有该科目的" + (examType == 1 ? "正式" : "模拟") + "考试预约，请勿重复操作");
        }

        Exam exam = new Exam();
        exam.setStudentId(student.getId());
        exam.setSubject(subject);
        exam.setExamType(examType);
        exam.setExamDate(examDate);
        exam.setExamSite(examSite);
        if (examType == 1) {
            exam.setStatus(1); // 正式考试直接预约成功
        } else {
            exam.setStatus(0); // 模拟考试待审核
        }
        examMapper.insert(exam);
    }

    /**
     * 获取学员自己的考试记录
     */
    public List<Exam> getMyExams(Long userId) {
        Student student = studentMapper.selectOne(new QueryWrapper<Student>().eq("user_id", userId));
        if (student == null) return List.of();
        return examMapper.selectByStudentId(student.getId());
    }

    /**
     * 学员取消考试预约
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelExam(Long userId, Long examId) {
        Student student = studentMapper.selectOne(new QueryWrapper<Student>().eq("user_id", userId));
        if (student == null) throw new RuntimeException("学员信息不存在");

        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new RuntimeException("记录不存在");
        }
        if (!exam.getStudentId().equals(student.getId())) {
            throw new RuntimeException("无权操作他人的预约");
        }
        if (exam.getStatus() != 0) {
            throw new RuntimeException("只有待审核的预约才能取消");
        }

        examMapper.deleteById(examId);
    }

    /**
     * 管理员：获取所有考试申请
     */
    public List<Exam> getAdminExamList(Integer status) {
        return examMapper.selectAdminExamList(status);
    }

    /**
     * 管理员：审核考试预约 (支持分配考场和日期)
     */
    public void auditExam(Long examId, Integer status, String examSite, Date examDate) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) throw new RuntimeException("记录不存在");
        
        exam.setStatus(status);
        if (examSite != null) exam.setExamSite(examSite);
        if (examDate != null) exam.setExamDate(examDate);
        
        examMapper.updateById(exam);
    }

    /**
     * 管理员：录入成绩
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordScore(Long examId, Integer score) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) throw new RuntimeException("记录不存在");

        exam.setScore(score);
        exam.setStatus(2); // 2-考试完成
        examMapper.updateById(exam);

        if (exam.getExamType() == null || exam.getExamType() == 1) {
            // 调用 ProgressService 的核心逻辑来更新进度，不再重复创建 Exam 记录
            progressService.updateProgressByExam(exam.getStudentId(), exam.getSubject(), score);
        }
    }
}
