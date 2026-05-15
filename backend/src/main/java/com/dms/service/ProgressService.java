package com.dms.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dms.entity.LearningProgress;
import com.dms.entity.Student;
import com.dms.entity.TrainingRecord;
import com.dms.mapper.LearningProgressMapper;
import com.dms.mapper.StudentMapper;
import com.dms.mapper.TrainingRecordMapper;
import com.dms.mapper.ExamMapper;
import com.dms.entity.Exam;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class ProgressService {

    private final LearningProgressMapper progressMapper;
    private final TrainingRecordMapper recordMapper;
    private final StudentMapper studentMapper;
    private final ExamMapper examMapper;

    public ProgressService(LearningProgressMapper progressMapper, 
                           TrainingRecordMapper recordMapper,
                           StudentMapper studentMapper,
                           ExamMapper examMapper) {
        this.progressMapper = progressMapper;
        this.recordMapper = recordMapper;
        this.studentMapper = studentMapper;
        this.examMapper = examMapper;
    }

    /**
     * 录入考试结果并更新进度
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordExamResult(Long studentId, Integer subject, Integer score, String remark) {
        // 1. 保存或更新考试记录
        Exam exam = new Exam();
        exam.setStudentId(studentId);
        exam.setSubject(subject);
        exam.setScore(score);
        exam.setExamDate(new Date());
        exam.setStatus(2); // 已完成
        exam.setExamSite("模拟考/校内考"); // 默认值
        examMapper.insert(exam);

        // 2. 如果分数合格，更新学习进度状态为“已通过 (2)”
        // 科目一和科目四 90分合格，科目二和科目三 80分合格 (假设)
        boolean passed = false;
        if ((subject == 1 || subject == 4) && score >= 90) passed = true;
        if ((subject == 2 || subject == 3) && score >= 80) passed = true;

        if (passed) {
            LearningProgress progress = progressMapper.selectOne(
                new QueryWrapper<LearningProgress>().eq("student_id", studentId).eq("subject", subject)
            );
            if (progress != null) {
                progress.setStatus(2); // 已通过
                progressMapper.updateById(progress);
            }
            
            // 如果是科目四通过，可以将学员状态改为“已拿证”
            if (subject == 4) {
                Student student = studentMapper.selectById(studentId);
                if (student != null) {
                    student.setStatus(3); // 已拿证
                    studentMapper.updateById(student);
                }
            }
        }
    }

    /**
     * 初始化学员进度 (科目一到科目四)
     */
    @Transactional(rollbackFor = Exception.class)
    public void initProgress(Long studentId) {
        for (int i = 1; i <= 4; i++) {
            LearningProgress progress = new LearningProgress();
            progress.setStudentId(studentId);
            progress.setSubject(i);
            progress.setHoursDone(0);
            progress.setStatus(0); // 进行中
            progressMapper.insert(progress);
        }
    }

    /**
     * 获取学员完整进度
     */
    public List<LearningProgress> getStudentProgress(Long userId) {
        Student student = studentMapper.selectOne(new QueryWrapper<Student>().eq("user_id", userId));
        if (student == null) return List.of();
        return progressMapper.selectByStudentId(student.getId());
    }

    /**
     * 录入学时记录并更新进度
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordTraining(Long instructorId, Long studentId, Integer subject, BigDecimal hours, String content) {
        // 1. 保存详细练车记录
        TrainingRecord record = new TrainingRecord();
        record.setStudentId(studentId);
        record.setInstructorId(instructorId);
        record.setSubject(subject);
        record.setTrainingDate(new Date());
        record.setHours(hours);
        record.setContent(content);
        recordMapper.insert(record);

        // 2. 更新汇总进度表
        progressMapper.addHours(studentId, subject, hours.intValue());
        
        // 3. 检查是否达到学时标准
        checkAndUpdateStatus(studentId, subject);
    }

    private void checkAndUpdateStatus(Long studentId, Integer subject) {
        LearningProgress progress = progressMapper.selectOne(
            new QueryWrapper<LearningProgress>().eq("student_id", studentId).eq("subject", subject)
        );
        if (progress == null) return;

        int requiredHours = switch (subject) {
            case 1 -> 12;
            case 2 -> 16;
            case 3 -> 24;
            case 4 -> 10;
            default -> 0;
        };

        if (progress.getHoursDone() >= requiredHours && progress.getStatus() == 0) {
            progress.setStatus(1); // 已达标/待考
            progressMapper.updateById(progress);
        }
    }
    
    public List<TrainingRecord> getMyRecords(Long userId) {
        Student student = studentMapper.selectOne(new QueryWrapper<Student>().eq("user_id", userId));
        if (student == null) return List.of();
        return recordMapper.selectByStudentId(student.getId());
    }
}
