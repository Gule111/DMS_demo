package com.dms.service;

import com.dms.mapper.StatisticsMapper;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

import com.dms.dto.DashboardStatsDTO;

@Service
public class StatisticsService {

    private final StatisticsMapper statisticsMapper;

    public StatisticsService(StatisticsMapper statisticsMapper) {
        this.statisticsMapper = statisticsMapper;
    }

    public DashboardStatsDTO getAdminDashboardData() {
        DashboardStatsDTO dto = new DashboardStatsDTO();
        dto.setOverview(statisticsMapper.getOverviewStats());
        dto.setEnrollmentTrend(statisticsMapper.getEnrollmentTrend());
        dto.setPassRates(statisticsMapper.getPassRates());
        dto.setCoachLoad(statisticsMapper.getCoachLoad());
        return dto;
    }
}
