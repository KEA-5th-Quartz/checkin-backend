package com.quartz.checkin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quartz.checkin.dto.statisitics.request.StatClosedRateRequest;
import com.quartz.checkin.dto.statisitics.request.StatProgressRequest;
import com.quartz.checkin.dto.statisitics.response.StatCategoryTicketResponse;
import com.quartz.checkin.dto.statisitics.response.StatClosedRateResponse;
import com.quartz.checkin.dto.statisitics.response.StatProgressResponse;
import com.quartz.checkin.dto.statisitics.response.StatTotalProgressResponse;
import com.quartz.checkin.repository.StatsTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

    // 1. 전체 진행 상태 통계 조회
    public StatTotalProgressResponse getStatTotalProgress() {
        List<Object[]> result = statsTicketRepository.findStatTotalProgress();
        if (result.isEmpty()) return new StatTotalProgressResponse();

        Object[] row = result.get(0);
        int deleted_count = ((Number) row[0]).intValue();
        String stateJson = (String) row[1];

        List<StatTotalProgressResponse.StatusCount> state = parseStateJson(stateJson);
        return new StatTotalProgressResponse(deleted_count, state);
    }

    // 2. 유형별 진행률 조회
    public List<StatProgressResponse> getProgressRates(StatProgressRequest request) {
        List<Map<String, Object>> result = statsTicketRepository.findProgressRatesByType(request);
        List<StatProgressResponse> response = new ArrayList<>();

        for (Map<String, Object> row : result) {
            String username = (String) row.get("username");
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

    // 3. 티켓 완료율 조회
    public StatClosedRateResponse getCompletionRate(StatClosedRateRequest request) {
        Optional<Double> closedRate = statsTicketRepository.findCompletionRateByType(request);
        return new StatClosedRateResponse(closedRate.orElse(0.0));
    }

    // 4. 카테고리별 티켓 통계 조회
    public List<StatCategoryTicketResponse> getCategoryTicketStats() {
        return statsTicketRepository.findCategoryTicketStats().stream()
                .map(result -> new StatCategoryTicketResponse(
                        (String) result[0],
                        ((Number) result[1]).longValue()
                ))
                .collect(Collectors.toList());
    }

    // JSON 파싱 유틸리티 메서드
    private List<StatTotalProgressResponse.StatusCount> parseStateJson(String stateJson) {
        try {
            return objectMapper.readValue(stateJson, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("JSON 파싱 실패", e);
        }
    }
}