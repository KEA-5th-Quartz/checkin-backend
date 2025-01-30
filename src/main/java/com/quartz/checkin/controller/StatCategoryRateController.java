package com.quartz.checkin.controller;



import com.quartz.checkin.dto.response.StatCategoryRateResponse;
import com.quartz.checkin.service.StatCategoryRateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;


@RestController
@RequestMapping("/stats")
public class StatCategoryRateController {

    @Autowired
    private StatCategoryRateService statCategoryRateService;

    @GetMapping("/managers/categories")
    public List<StatCategoryRateResponse> getStatsByCategory() {
        return statCategoryRateService.getStatsByCategory();
    }
}