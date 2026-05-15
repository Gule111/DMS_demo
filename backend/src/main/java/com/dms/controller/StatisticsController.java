package com.dms.controller;

import com.dms.common.Result;
import com.dms.service.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

import com.dms.dto.DashboardStatsDTO;

@RestController
@RequestMapping("/stats")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/admin/dashboard")
    public Result<DashboardStatsDTO> getAdminDashboard() {
        return Result.success(statisticsService.getAdminDashboardData());
    }
}
