package com.quartz.checkin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quartz.checkin.dto.statisitics.response.StatCategoryTicketResponse;
import com.quartz.checkin.dto.statisitics.response.StatClosedRateResponse;
import com.quartz.checkin.dto.statisitics.response.StatProgressResponse;
import com.quartz.checkin.repository.StatsTicketRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    // 2. 유형별 진행률 조회
    public List<StatProgressResponse> getProgressRates(String type) {
        List<Map<String, Object>> result = statsTicketRepository.findProgressRatesByType(type);
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
    public StatClosedRateResponse getCompletionRate(String type) {
        Optional<Double> closedRate = statsTicketRepository.findCompletionRateByType(type);
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