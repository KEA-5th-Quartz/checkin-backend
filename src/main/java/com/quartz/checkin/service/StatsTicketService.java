package com.quartz.checkin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quartz.checkin.dto.statisitics.response.StatCategoryTicketResponse;
import com.quartz.checkin.dto.statisitics.response.StatClosedRateResponse;
import com.quartz.checkin.dto.statisitics.response.StatProgressResponse;
import com.quartz.checkin.repository.StatsTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Optional;

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

    // 작업 완성률 조회
    public StatClosedRateResponse getCompletionRate(String type) {
        Optional<Double> closedRate = statsTicketRepository.findCompletionRateByType(type);

        double rate = closedRate.orElse(0.0);
        rate = Math.max(0.0, Math.min(rate, 100.0));
        return new StatClosedRateResponse(rate);
    }

    // 카테고리별 진행중인 티켓 수 조회
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