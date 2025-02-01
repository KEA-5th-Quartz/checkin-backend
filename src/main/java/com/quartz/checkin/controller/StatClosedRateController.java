package com.quartz.checkin.controller;

import com.quartz.checkin.dto.request.StatClosedRateRequest;
import com.quartz.checkin.dto.response.StatClosedRateResponse;
import com.quartz.checkin.service.StatClosedRateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stats")
public class StatClosedRateController {

    @Autowired
    private StatClosedRateService statClosedRateService;

    @GetMapping("/closed-rate")
    public StatClosedRateResponse getCompletionRate(@RequestBody StatClosedRateRequest request) {
        return statClosedRateService.getCompletionRate(request);
    }
}