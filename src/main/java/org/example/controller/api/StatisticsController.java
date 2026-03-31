package org.example.controller.api;

import org.example.dto.response.ApiResponse;
import org.example.service.search.StatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST API для статистики базы данных
 */
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * Получить статистику базы данных
     * GET /api/statistics
     */
    @GetMapping
    public ApiResponse<Map<String, Long>> getStatistics() {
        return ApiResponse.success(statisticsService.getStatistics());
    }
}
