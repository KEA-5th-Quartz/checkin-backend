package com.quartz.checkin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quartz.checkin.dto.response.StatTotalProgressResponse;
import com.quartz.checkin.repository.StatTotalProgressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;


@Service
public class StatTotalProgressService {

    @Autowired
    private StatTotalProgressRepository statTotalProgressRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public StatTotalProgressResponse getStatTotalProgress() {
        List<Object[]> result = statTotalProgressRepository.findStatTotalProgress();

        if (result.isEmpty()) {
            return new StatTotalProgressResponse();
        }

        Object[] row = result.get(0);
        int deleted_count = ((Number) row[0]).intValue();
        String stateJson = (String) row[1];

        List<StatTotalProgressResponse.StatusCount> state = parseStateJson(stateJson);

        StatTotalProgressResponse response = new StatTotalProgressResponse();
        response.setDeleted_count(deleted_count);
        response.setState(state);

        return response;
    }

    private List<StatTotalProgressResponse.StatusCount> parseStateJson(String stateJson) {
        try {
            return objectMapper.readValue(stateJson, new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse state JSON", e);
        }
    }
}