package com.quartz.checkin.controller;

import com.quartz.checkin.dto.request.StatProgressRequest;
import com.quartz.checkin.dto.response.StatProgressResponse;
import com.quartz.checkin.service.StatProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stats")
public class StatProgressController {

    @Autowired
    private StatProgressService statProgressService;

    @GetMapping("/managers")
    public List<StatProgressResponse> getProgressRates(@RequestBody StatProgressRequest request) {
        return statProgressService.getProgressRates(request);
    }
}