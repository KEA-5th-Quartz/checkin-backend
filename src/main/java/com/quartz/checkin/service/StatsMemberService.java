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
import java.util.*;

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

    // 2. 카테고리별 진행률 조회 (soft delete된 담당자 제외)
    public List<StatCategoryRateResponse> getStatsByCategory() {
        List<Map<String, Object>> result = statsMemberRepository.findStatsByCategory();
        List<StatCategoryRateResponse> response = new ArrayList<>();

        for (Map<String, Object> row : result) {
            String username = (String) row.get("userName");

            // soft delete된 담당자 필터링 (username이 null이거나 비어있는 경우 제외)
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

    // 3. 전체 진행률 조회 (soft delete된 담당자 제외)
    public List<StatTotalProgressResponse> getStatTotalProgress() {
        // NativeQuery 결과를 List<Object[]>로 받음
        List<Object[]> queryResults = statsMemberRepository.findStatTotalProgress();

        if (queryResults.isEmpty() || queryResults.get(0) == null) {
            return Collections.emptyList();
        }

        // 결과 파싱
        Object[] result = queryResults.get(0);
        int overdueCount = ((Number) result[0]).intValue(); // OVERDUE 값
        String stateJson = (String) result[1]; // state JSON 문자열

        try {
            // state JSON 파싱
            List<StatTotalProgressResponse> stateList = objectMapper.readValue(
                    stateJson,
                    objectMapper.getTypeFactory().constructCollectionType(
                            List.class,
                            StatTotalProgressResponse.class
                    )
            );

            // OVERDUE 값을 stateList에 추가
            stateList.add(new StatTotalProgressResponse("OVERDUE", overdueCount));
            return stateList;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON response", e);
        }
    }
}
