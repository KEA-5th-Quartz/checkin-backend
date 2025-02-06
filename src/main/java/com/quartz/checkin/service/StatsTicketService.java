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

   /* public StatTotalProgressResponse getStatTotalProgress() {
        List<Object[]> result = statsTicketRepository.findStatTotalProgress();
        if (result.isEmpty()) return new StatTotalProgressResponse();

        Object[] row = result.get(0);
        int overdue = ((Number) row[0]).intValue(); // 첫 번째 컬럼이 overdue
        String stateJson = (String) row[1]; // 두 번째 컬럼이 state_json

        List<StatTotalProgressResponse.StatusCount> state = parseStateJson(stateJson);
        return new StatTotalProgressResponse(overdue, state);
    }*/

    // 2. 유형별 진행률 조회
    public List<StatProgressResponse> getProgressRates(StatProgressRequest request) {
        List<Map<String, Object>> result = statsTicketRepository.findProgressRatesByType(request);
        List<StatProgressResponse> response = new ArrayList<>();

        for (Map<String, Object> row : result) {
            String username = (String) row.get("userName");
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

    // 4. 담당자의 카테고리별 티켓수
    public List<StatCategoryTicketResponse> getCategoryTicketStats() {
        return statsTicketRepository.findCategoryTicketStats().stream()
                .map(result -> new StatCategoryTicketResponse(
                        (String) result[0],
                        ((Number) result[1]).longValue()
                ))
                .collect(Collectors.toList());
    }


}