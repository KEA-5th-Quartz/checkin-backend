package com.quartz.checkin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quartz.checkin.dto.stat.response.StatCategoryTicketResponse;
import com.quartz.checkin.dto.stat.response.StatProgressResponse;
import com.quartz.checkin.repository.StatsTicketRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatsTicketService {

    private final StatsTicketRepository statsTicketRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public StatsTicketService(
            StatsTicketRepository statsTicketRepository,
            ObjectMapper objectMapper
    ) {
        this.statsTicketRepository = statsTicketRepository;
        this.objectMapper = objectMapper;
    }

    // 각 담당자의 상태별 티켓 수 (soft delete된 담당자 제외)
    public List<StatProgressResponse> getProgressRates(String type) {
        List<Map<String, Object>> result = statsTicketRepository.findProgressRatesByType(type);
        List<StatProgressResponse> response = new ArrayList<>();

        for (Map<String, Object> row : result) {
            String username = (String) row.get("userName");

            if (username == null || username.isEmpty()) {
                continue;
            }

            String stateJson = (String) row.get("state");

            try {
                List<StatProgressResponse.StatusRate> state = objectMapper.readValue(
                        stateJson,
                        new TypeReference<>() {}
                );

                response.add(new StatProgressResponse(username, state));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return response;
    }

    public List<StatCategoryTicketResponse> getCategoryTicketStats() {
        return statsTicketRepository.findCategoryTicketStats().stream()
                .filter(result ->
                        result[0] != null &&
                                result[1] != null &&
                                ((Number) result[1]).longValue() > 0
                )
                .map(result -> new StatCategoryTicketResponse(
                        (String) result[0],
                        ((Number) result[1]).longValue()
                ))
                .collect(Collectors.toList());
    }
}