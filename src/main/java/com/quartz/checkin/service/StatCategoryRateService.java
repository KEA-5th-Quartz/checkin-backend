package com.quartz.checkin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quartz.checkin.dto.response.StatCategoryCount;
import com.quartz.checkin.dto.response.StatCategoryRateResponse;
import com.quartz.checkin.repository.StatCategoryRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StatCategoryRateService {

    @Autowired
    private StatCategoryRateRepository statCategoryRateRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public List<StatCategoryRateResponse> getStatsByCategory() {
        List<Map<String, Object>> result = statCategoryRateRepository.findStatsByCategory();
        List<StatCategoryRateResponse> response = new ArrayList<>();

        for (Map<String, Object> row : result) {
            String username = (String) row.get("username");
            String stateJson = (String) row.get("state");

            try {
                // JSON 문자열을 List<StatCategoryCount>로 변환
                List<StatCategoryCount> state = objectMapper.readValue(
                        stateJson,
                        new TypeReference<List<StatCategoryCount>>() {}
                );

                // StatCategoryRateResponse 객체 생성
                response.add(new StatCategoryRateResponse(username, state));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return response;
    }
}