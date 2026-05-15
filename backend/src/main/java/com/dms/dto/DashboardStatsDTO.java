package com.dms.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DashboardStatsDTO {
    private OverviewStats overview;
    private List<TrendStatDTO> enrollmentTrend;
    private List<Map<String, Object>> passRates;
    private List<Map<String, Object>> coachLoad;

    @Data
    public static class OverviewStats {
        private Integer totalStudents;
        private Integer totalInstructors;
        private Integer pendingExams;
        private Integer pendingEnrollments;
    }
}
