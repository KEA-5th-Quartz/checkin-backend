package com.quartz.checkin.controller;

import com.quartz.checkin.dto.common.response.ApiResponse;
import com.quartz.checkin.dto.statisitics.request.StatClosedRateRequest;
import com.quartz.checkin.dto.statisitics.request.StatProgressRequest;
import com.quartz.checkin.dto.statisitics.response.*;
import com.quartz.checkin.security.annotation.AdminOrManager;
import com.quartz.checkin.security.annotation.Manager;
import com.quartz.checkin.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/stats")
public class StatisticsController {

    private final StatsMemberService statsMemberService;
    private final StatsTicketService statsTicketService;

    public StatisticsController(StatsMemberService statsMemberService, StatsTicketService statsTicketService) {
        this.statsMemberService = statsMemberService;
        this.statsTicketService = statsTicketService;
    }

    @AdminOrManager
    @GetMapping("/managers")
    public ApiResponse<List<StatProgressResponse>> getProgressRates(@RequestBody StatProgressRequest request) {
        List<StatProgressResponse> response = statsTicketService.getProgressRates(request);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @AdminOrManager
    @GetMapping("/categories")
    public ApiResponse<List<StatCategoryTicketResponse>> getCategoryTicketStats() {
        List<StatCategoryTicketResponse> response = statsTicketService.getCategoryTicketStats();
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @AdminOrManager
    @GetMapping("/status-rate")
    public ApiResponse<List<StatTotalProgressResponse>> getTotalProgress() {
        List<StatTotalProgressResponse> response = statsMemberService.getStatTotalProgress();
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }


    @AdminOrManager
    @GetMapping("/closed-rate")
    public ApiResponse<StatClosedRateResponse> getCompletionRate(@RequestBody StatClosedRateRequest request) {
        StatClosedRateResponse response = statsTicketService.getCompletionRate(request);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Manager
    @GetMapping("/{managerId}/due-today")
    public ApiResponse<StatDueTodayResponse> getTicketCount(@PathVariable Long managerId) {
        StatDueTodayResponse response = statsMemberService.getTicketCountByManagerId(managerId);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Manager
    @GetMapping("/managers/categories")
    public ApiResponse<List<StatCategoryRateResponse>> getStatsByCategory() {
        List<StatCategoryRateResponse> response = statsMemberService.getStatsByCategory();
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }
}