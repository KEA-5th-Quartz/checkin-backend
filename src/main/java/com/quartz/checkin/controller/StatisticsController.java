package com.quartz.checkin.controller;

import com.quartz.checkin.dto.statisitics.request.StatClosedRateRequest;
import com.quartz.checkin.dto.statisitics.request.StatProgressRequest;
import com.quartz.checkin.dto.statisitics.response.*;
import com.quartz.checkin.security.annotation.Admin;
import com.quartz.checkin.security.annotation.AdminOrManager;
import com.quartz.checkin.security.annotation.Manager;
import com.quartz.checkin.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatsTicketService statTotalProgressService;
    private final StatsTicketService statProgressService;
    private final StatsTicketService statClosedRateService;
    private final StatsTicketService statCategoryTicketService;
    private final StatsMemberService statCategoryRateService;
    private final StatsMemberService statDueTodayService;

    @AdminOrManager
    @GetMapping("/managers")
    public List<StatProgressResponse> getProgressRates(@RequestBody StatProgressRequest request) {
        return statProgressService.getProgressRates(request);
    }

    @AdminOrManager
    @GetMapping("/categories")
    public ResponseEntity<List<StatCategoryTicketResponse>> getCategoryTicketStats() {
        List<StatCategoryTicketResponse> response = statCategoryTicketService.getCategoryTicketStats();
        return ResponseEntity.ok(response);
    }

    @AdminOrManager
    @GetMapping("/status-rate")
    public ResponseEntity<StatTotalProgressResponse> getStatTotalProgress() {
        StatTotalProgressResponse response = statTotalProgressService.getStatTotalProgress();
        return ResponseEntity.ok(response);
    }

    @AdminOrManager
    @GetMapping("/closed-rate")
    public StatClosedRateResponse getCompletionRate(@RequestBody StatClosedRateRequest request) {
        return statClosedRateService.getCompletionRate(request);
    }

    @Manager
    @GetMapping("/{managerId}/due-today")
    public StatDueTodayResponse getTicketCount(@PathVariable Long managerId) { // @RequestParam → @PathVariable
        return statDueTodayService.getTicketCountByManagerId(managerId);
    }

    @Manager
    @GetMapping("/managers/categories")
    public List<StatCategoryRateResponse> getStatsByCategory() {
        return statCategoryRateService.getStatsByCategory();
    }
}