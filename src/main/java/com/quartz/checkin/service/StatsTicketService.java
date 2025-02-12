package com.quartz.checkin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quartz.checkin.dto.statisitics.response.StatCategoryTicketResponse;
import com.quartz.checkin.dto.statisitics.response.StatClosedRateResponse;
import com.quartz.checkin.dto.statisitics.response.StatProgressResponse;
import com.quartz.checkin.repository.StatsTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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

    // 2. 유형별 진행률 조회 (soft delete된 담당자 제외)
    public List<StatProgressResponse> getProgressRates(String type) {
        List<Map<String, Object>> result = statsTicketRepository.findProgressRatesByType(type);
        List<StatProgressResponse> response = new ArrayList<>();

        for (Map<String, Object> row : result) {
            String username = (String) row.get("userName");

            // soft delete된 담당자 필터링 (username이 null이거나 비어있는 경우 제외)
            if (username == null || username.isEmpty()) {
                continue;
            }

            String stateJson = (String) row.get("state");

            try {
                List<StatProgressResponse.StatusRate> state = objectMapper.readValue(
                        stateJson,
                        new TypeReference<>() {}
                );

                // 상태 데이터 추가 필터링 (필요 시)
                response.add(new StatProgressResponse(username, state));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return response;
    }

    // 3. 티켓 완료율 조회 (추가 필터링)
    public StatClosedRateResponse getCompletionRate(String type) {
        Optional<Double> closedRate = statsTicketRepository.findCompletionRateByType(type);

        // 음수 값 방지 및 기본값 처리
        double rate = closedRate.orElse(0.0);
        rate = Math.max(0.0, Math.min(rate, 100.0)); // 0~100% 범위 강제
        return new StatClosedRateResponse(rate);
    }

    // 4. 담당자의 카테고리별 티켓수 (null 값 필터링 강화)
    public List<StatCategoryTicketResponse> getCategoryTicketStats() {
        return statsTicketRepository.findCategoryTicketStats().stream()
                .filter(result ->
                        result[0] != null &&  // 카테고리 이름 null 체크
                                result[1] != null &&   // 티켓 개수 null 체크
                                ((Number) result[1]).longValue() > 0 // 0건 이하 제외
                )
                .map(result -> new StatCategoryTicketResponse(
                        (String) result[0],
                        ((Number) result[1]).longValue()
                ))
                .collect(Collectors.toList());
    }
}