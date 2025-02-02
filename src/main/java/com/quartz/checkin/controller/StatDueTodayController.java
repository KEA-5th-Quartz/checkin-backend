package com.quartz.checkin.controller;

import com.quartz.checkin.dto.response.StatDueTodayResponse;
import com.quartz.checkin.service.StatDueTodayService;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;


@RestController
@RequestMapping("/stats")
public class StatDueTodayController {

    private final StatDueTodayService StatDueTodayService;

    public StatDueTodayController(StatDueTodayService StatDueTodayService) {
        this.StatDueTodayService = StatDueTodayService;
    }

    @GetMapping("/{managerId}/due-today")
    public StatDueTodayResponse getTicketCount(
            @RequestParam Long managerId) {  // 쿼리 파라미터 ?managerId=3
        // managerId와 managerIdParam 중 어떤 것을 사용할지 결정
        // 예: managerIdParam을 사용
        return StatDueTodayService.getTicketCountByManagerId(managerId);
    }
}