package com.quartz.checkin.controller;

import com.quartz.checkin.dto.response.StatTotalProgressResponse;
import com.quartz.checkin.service.StatTotalProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
public class StatTotalProgressController {

    private final StatTotalProgressService statTotalProgressService;

    public StatTotalProgressController(StatTotalProgressService statTotalProgressService) {
        this.statTotalProgressService = statTotalProgressService;
    }

    @GetMapping("/status-rate")
    public ResponseEntity<StatTotalProgressResponse> getStatTotalProgress() {
        StatTotalProgressResponse response = statTotalProgressService.getStatTotalProgress();
        return ResponseEntity.ok(response);
    }
}