package com.quartz.checkin.controller;


import com.quartz.checkin.dto.response.StatCategoryRateResponse;
import com.quartz.checkin.service.StatCategoryRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatCategoryRateController {

    private final StatCategoryRateService statCategoryRateService;

    @GetMapping("/category-rate")
    public List<StatCategoryRateResponse> getStatCategoryRates() {
        return statCategoryRateService.getStatCategoryRates();
    }
}
