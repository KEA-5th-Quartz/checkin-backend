package com.quartz.checkin.controller;

import com.quartz.checkin.dto.response.StatCategoryTicketResponse;
import com.quartz.checkin.service.StatCategoryTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stats") // 기본 경로를 "/stats"로 설정
public class StatCategoryTicketController {

    @Autowired
    private StatCategoryTicketService statCategoryTicketService;

    @GetMapping("/categories") // "/categories" 경로로 매핑
    public ResponseEntity<List<StatCategoryTicketResponse>> getCategoryTicketStats() {
        // Service 호출
        List<StatCategoryTicketResponse> response = statCategoryTicketService.getCategoryTicketStats();

        // 클라이언트에게 응답
        return ResponseEntity.ok(response);
    }
}