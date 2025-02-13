package com.quartz.checkin.controller;

import com.quartz.checkin.dto.common.response.ApiResponse;
import com.quartz.checkin.dto.stat.response.StatCategoryRateResponse;
import com.quartz.checkin.dto.stat.response.StatCategoryTicketResponse;
import com.quartz.checkin.dto.stat.response.StatClosedRateResponse;
import com.quartz.checkin.dto.stat.response.StatProgressResponse;
import com.quartz.checkin.dto.stat.response.StatTotalProgressResultResponse;
import com.quartz.checkin.security.annotation.Admin;
import com.quartz.checkin.security.annotation.AdminOrManager;
import com.quartz.checkin.security.annotation.Manager;
import com.quartz.checkin.service.StatsService;
import com.quartz.checkin.service.StatsTicketService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
public class StatisticsController {

    private final StatsService statsService;
    private final StatsTicketService statsTicketService;

    public StatisticsController(StatsService statsService, StatsTicketService statsTicketService) {
        this.statsService = statsService;
        this.statsTicketService = statsTicketService;
    }

    @AdminOrManager
    @Operation(summary = "API 명세서 v0.3 line 66", description = "각 담당자의 상태별 티켓수(type params 필요)- 세로 막대그래프")
    @GetMapping("/managers")
    public ApiResponse<List<StatProgressResponse>> getProgressRates(
            @RequestParam(name = "type") String type
    ) {
        List<StatProgressResponse> response = statsTicketService.getProgressRates(type);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @AdminOrManager
    @Operation(summary = "API 명세서 v0.3 line 67", description = "카테고리별 진행중인 티켓수 - 세로 막대그래프")
    @GetMapping("/categories")
    public ApiResponse<List<StatCategoryTicketResponse>> getCategoryTicketStats() {
        List<StatCategoryTicketResponse> response = statsTicketService.getCategoryTicketStats();
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Admin
    @Operation(summary = "API 명세서 v0.3 line 69", description = "전체 작업상태 분포 - 도넛 그래프")
    @GetMapping("/status-rate")
    public ApiResponse<StatTotalProgressResultResponse> getTotalProgress() {
        StatTotalProgressResultResponse response = statsService.getStatTotalProgress();
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }


    @Admin
    @Operation(summary = "API 명세서 v0.3 line 68", description = "작업 완성률 조회(type params 필요) - 도넛 그래프")
    @GetMapping("/closed-rate")
    public ApiResponse<StatClosedRateResponse> getCompletionRate(
            @RequestParam(name = "type") String type
    ) {
        StatClosedRateResponse response = statsTicketService.getCompletionRate(type);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Manager
    @Operation(summary = "API 명세서 v0.3 line 65", description = "각 담당자의 카테고리별 티켓수 - 가로 누적그래프")
    @GetMapping("/managers/categories")
    public ApiResponse<List<StatCategoryRateResponse>> getStatsByCategory() {
        List<StatCategoryRateResponse> response = statsService.getStatsByCategory();
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }
}