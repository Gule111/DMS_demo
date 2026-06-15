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
        // 如果是从 ExamService 过来的，可能已经有 Exam 记录了。
        // 这里我们默认该方法用于“手动录入/补录”，所以还是保留插入逻辑，但如果是官方考试流程，我们会另行处理。
        // 为了兼容性，我们保留原样，但在 ExamService 中我们直接调用 progress 逻辑。
        
        // 1. 保存或更新考试记录 (仅当没有传入已有 Exam ID 时，这里我们保持原样)
        Exam exam = new Exam();
        exam.setStudentId(studentId);
        exam.setSubject(subject);
        exam.setScore(score);
        exam.setExamDate(new Date());
        exam.setStatus(2); // 已完成
        exam.setExamSite("模拟考/校内考"); 
        examMapper.insert(exam);

        updateProgressByExam(studentId, subject, score);
    }

    /**
     * 核心逻辑：根据考试成绩更新科目进度与学员状态
     */
    public void updateProgressByExam(Long studentId, Integer subject, Integer score) {
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
     * 根据学员ID直接获取其完整学习进度
     */
    public List<com.dms.dto.LearningProgressDTO> getStudentProgressByStudentId(Long studentId) {
        List<LearningProgress> dbList = progressMapper.selectByStudentId(studentId);
        List<Exam> examList = examMapper.selectByStudentId(studentId);
        
        java.util.List<com.dms.dto.LearningProgressDTO> fullList = new java.util.ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            final int subject = i;
            
            LearningProgress dbProgress = dbList.stream()
                .filter(p -> p.getSubject() == subject)
                .findFirst()
                .orElse(null);
                
            com.dms.dto.LearningProgressDTO dto = new com.dms.dto.LearningProgressDTO();
            dto.setStudentId(studentId);
            dto.setSubject(subject);
            
            if (dbProgress != null) {
                dto.setId(dbProgress.getId());
                dto.setHoursDone(dbProgress.getHoursDone());
                dto.setStatus(dbProgress.getStatus());
            } else {
                dto.setHoursDone(0);
                dto.setStatus(0);
            }
            
            examList.stream()
                .filter(e -> e.getSubject() == subject && e.getScore() != null)
                .findFirst()
                .ifPresent(e -> dto.setLatestScore(e.getScore()));
                
            fullList.add(dto);
        }
        return fullList;
    }

    /**
     * 获取学员完整进度 (确保返回 1-4 科目完整列表，并附带成绩)
     */
    public List<com.dms.dto.LearningProgressDTO> getStudentProgress(Long userId) {
        Student student = studentMapper.selectOne(new QueryWrapper<Student>().eq("user_id", userId));
        if (student == null) return List.of();
        
        List<LearningProgress> dbList = progressMapper.selectByStudentId(student.getId());
        List<Exam> examList = examMapper.selectByStudentId(student.getId());
        
        java.util.List<com.dms.dto.LearningProgressDTO> fullList = new java.util.ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            final int subject = i;
            
            LearningProgress dbProgress = dbList.stream()
                .filter(p -> p.getSubject() == subject)
                .findFirst()
                .orElse(null);
                
            com.dms.dto.LearningProgressDTO dto = new com.dms.dto.LearningProgressDTO();
            dto.setStudentId(student.getId());
            dto.setSubject(subject);
            
            if (dbProgress != null) {
                dto.setId(dbProgress.getId());
                dto.setHoursDone(dbProgress.getHoursDone());
                dto.setStatus(dbProgress.getStatus());
            } else {
                dto.setHoursDone(0);
                dto.setStatus(0);
            }
            
            // 查找最新成绩 (examList 已经按日期 DESC 排序)
            examList.stream()
                .filter(e -> e.getSubject() == subject && e.getScore() != null)
                .findFirst()
                .ifPresent(e -> dto.setLatestScore(e.getScore()));
                
            fullList.add(dto);
        }
        return fullList;
    }

    /**
     * 录入学时记录并更新进度
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordTraining(Long instructorId, Long studentId, Integer subject, BigDecimal hours, String content) {
        // 校验前置科目：录入科目二和科目三学时前，学员必须已通过科目一考试 (状态为 2)
        if (subject == 2 || subject == 3) {
            LearningProgress sub1Progress = progressMapper.selectOne(
                new QueryWrapper<LearningProgress>().eq("student_id", studentId).eq("subject", 1)
            );
            if (sub1Progress == null || sub1Progress.getStatus() != 2) {
                throw new RuntimeException("该学员尚未通过科目一考试，无法录入后续科目学时");
            }
        }
        
        // 1. 保存详细练车记录
        TrainingRecord record = new TrainingRecord();
        record.setStudentId(studentId);
        record.setInstructorId(instructorId);
        record.setSubject(subject);
        record.setTrainingDate(new Date());
        record.setHours(hours);
        record.setContent(content);
        recordMapper.insert(record);

        // 2. 确保汇总进度表中存在该科目的记录
        LearningProgress progress = progressMapper.selectOne(
            new QueryWrapper<LearningProgress>().eq("student_id", studentId).eq("subject", subject)
        );
        if (progress == null) {
            progress = new LearningProgress();
            progress.setStudentId(studentId);
            progress.setSubject(subject);
            progress.setHoursDone(hours.intValue());
            progress.setStatus(0);
            progressMapper.insert(progress);
        } else {
            // 更新汇总进度表
            progressMapper.addHours(studentId, subject, hours.intValue());
        }
        
        // 3. 检查是否达到学时标准 (重新查询最新学时以确保准确)
        checkAndUpdateStatus(studentId, subject);
    }

    private void checkAndUpdateStatus(Long studentId, Integer subject) {
        LearningProgress progress = progressMapper.selectOne(
            new QueryWrapper<LearningProgress>().eq("student_id", studentId).eq("subject", subject)
        );
        if (progress == null) return;

        int requiredHours = switch (subject) {
            case 1 -> 12; // 科目一理论课也计入学时要求 (12小时)
            case 2 -> 16;
            case 3 -> 24;
            case 4 -> 0;  // 理论课，无需强制实操学时
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
