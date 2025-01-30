package com.quartz.checkin.controller;

import com.quartz.checkin.dto.response.StatDueTodayResponse;
import com.quartz.checkin.service.StatDueTodayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatDueTodayController {

    private final StatDueTodayService statDueTodayService;

    @GetMapping("/{managerId}/due-today")
    public ResponseEntity<StatDueTodayResponse> getDueTodayStats(@PathVariable Long managerId) {
        StatDueTodayResponse stats = statDueTodayService.getDueTodayTickets(managerId);
        return ResponseEntity.ok(stats);
    }
}
