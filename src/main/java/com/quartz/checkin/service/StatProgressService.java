package com.quartz.checkin.service;

import com.quartz.checkin.dto.request.StatProgressRequest;
import com.quartz.checkin.dto.response.StatProgressResponse;
import com.quartz.checkin.repository.StatProgressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StatProgressService {

    @Autowired
    private StatProgressRepository statProgressRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public List<StatProgressResponse> getProgressRates(StatProgressRequest request) {
        List<Map<String, Object>> result = statProgressRepository.findProgressRatesByType(request);
        List<StatProgressResponse> response = new ArrayList<>();

        for (Map<String, Object> row : result) {
            String username = (String) row.get("username");
            String stateJson = (String) row.get("state");

            try {
                // JSON 문자열을 List<StatusRate>로 변환
                List<StatProgressResponse.StatusRate> state = objectMapper.readValue(
                        stateJson,
                        new TypeReference<List<StatProgressResponse.StatusRate>>() {}
                );

                // ProgressResponse 객체 생성
                response.add(new StatProgressResponse(username, state));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return response;
    }
}