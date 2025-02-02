package com.quartz.checkin.controller;



import com.quartz.checkin.dto.response.StatCategoryRateResponse;
import com.quartz.checkin.service.StatCategoryRateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;


@RestController
@RequestMapping("/stats")
public class StatCategoryRateController {

    private final StatCategoryRateService statCategoryRateService;

    public StatCategoryRateController(StatCategoryRateService statCategoryRateService) {
        this.statCategoryRateService = statCategoryRateService;
    }

    @GetMapping("/managers/categories")
    public List<StatCategoryRateResponse> getStatsByCategory() {
        return statCategoryRateService.getStatsByCategory();
    }
}