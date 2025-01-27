package com.quartz.checkin.service;

import com.quartz.checkin.dto.response.CategoryTicketStatsResponse;
import com.quartz.checkin.repository.StatCategoryTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatCategoryTicketService {

    @Autowired
    private StatCategoryTicketRepository statCategoryTicketRepository;

    public List<CategoryTicketStatsResponse> getCategoryTicketStats() {
        // Repository 호출
        List<Object[]> results = statCategoryTicketRepository.findCategoryTicketStats();

        // 결과를 Response DTO로 변환
        return results.stream()
                .map(result -> new CategoryTicketStatsResponse(
                        (String) result[0],  // categoryName
                        ((Number) result[1]).longValue()  // ticketCount
                ))
                .collect(Collectors.toList());
    }
}
