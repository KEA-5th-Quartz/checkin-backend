package com.quartz.checkin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quartz.checkin.dto.statisitics.response.StatCategoryCountResponse;
import com.quartz.checkin.dto.statisitics.response.StatCategoryRateResponse;
import com.quartz.checkin.dto.statisitics.response.StatTotalProgressResponse;
import com.quartz.checkin.repository.StatsMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class StatsMemberService {

    private final StatsMemberRepository statsMemberRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public StatsMemberService(
            StatsMemberRepository statsMemberRepository,
            ObjectMapper objectMapper
    ) {
        this.statsMemberRepository = statsMemberRepository;
        this.objectMapper = objectMapper;
    }

    // 각 담당자의 카테고리별 티켓 수
    public List<StatCategoryRateResponse> getStatsByCategory() {
        List<Map<String, Object>> result = statsMemberRepository.findStatsByCategory();
        List<StatCategoryRateResponse> response = new ArrayList<>();

        for (Map<String, Object> row : result) {
            String username = (String) row.get("userName");


            if (username == null || username.isEmpty()) {
                continue;
            }

            String stateJson = (String) row.get("state");

            try {
                List<StatCategoryCountResponse> state = objectMapper.readValue(
                        stateJson,
                        new TypeReference<>() {}
                );
                response.add(new StatCategoryRateResponse(username, state));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return response;
    }

    // 전체 작업상태분포(OVERDUE 포함)
    public List<StatTotalProgressResponse> getStatTotalProgress() {

        List<Object[]> queryResults = statsMemberRepository.findStatTotalProgress();

        if (queryResults.isEmpty() || queryResults.get(0) == null) {
            return Collections.emptyList();
        }

        Object[] result = queryResults.get(0);
        int overdueCount = ((Number) result[0]).intValue();
        String stateJson = (String) result[1];

        try {
            List<StatTotalProgressResponse> stateList = objectMapper.readValue(
                    stateJson,
                    objectMapper.getTypeFactory().constructCollectionType(
                            List.class,
                            StatTotalProgressResponse.class
                    )
            );

            stateList.add(new StatTotalProgressResponse("OVERDUE", overdueCount));
            return stateList;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON response", e);
        }
    }
}
