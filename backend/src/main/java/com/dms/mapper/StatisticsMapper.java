package com.dms.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

import com.dms.dto.DashboardStatsDTO;

import com.dms.dto.TrendStatDTO;

@Mapper
public interface StatisticsMapper {

    /**
     * 近7天报名人数统计 (格式化日期)
     */
    @Select("SELECT DATE_FORMAT(created_at, '%Y-%m-%d') as statDate, COUNT(*) as count " +
            "FROM sys_users " +
            "WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
            "GROUP BY statDate ORDER BY statDate ASC")
    List<TrendStatDTO> getEnrollmentTrend();

    /**
     * 各科目考试通过率统计
     */
    @Select("SELECT subject, " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN (subject IN (1,4) AND score >= 90) OR (subject IN (2,3) AND score >= 80) THEN 1 ELSE 0 END) as passed " +
            "FROM biz_exams WHERE status = 2 " +
            "GROUP BY subject")
    List<Map<String, Object>> getPassRates();

    /**
     * 教练带教负荷分布
     */
    @Select("SELECT real_name as name, current_load as value FROM biz_instructors")
    List<Map<String, Object>> getCoachLoad();
    
    /**
     * 总体数据概览 (映射到 DTO)
     */
    @Select("SELECT " +
            "(SELECT COUNT(*) FROM biz_students) as totalStudents, " +
            "(SELECT COUNT(*) FROM biz_instructors) as totalInstructors, " +
            "(SELECT COUNT(*) FROM biz_exams WHERE status = 0) as pendingExams, " +
            "(SELECT COUNT(*) FROM biz_enrollments WHERE audit_status = 0) as pendingEnrollments")
    DashboardStatsDTO.OverviewStats getOverviewStats();
}
