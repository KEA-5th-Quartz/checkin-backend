package com.quartz.checkin.service;

import com.quartz.checkin.dto.response.StatCategoryRateResponse;
import com.quartz.checkin.repository.StatCategoryRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatCategoryRateService {

    private final StatCategoryRateRepository statCategoryRateRepository;

    public List<StatCategoryRateResponse> getStatCategoryRates() {
        List<Object[]> results = statCategoryRateRepository.getStatCategoryRate();
        List<StatCategoryRateResponse> responseList = new ArrayList<>();

        for (Object[] row : results) {
            String username = (String) row[0];
            String jsonArray = (String) row[1];

            List<StatCategoryRateResponse.CategoryState> categoryStates = new ArrayList<>();
            List<Map<String, Object>> parsedList = parseJsonArray(jsonArray);

            for (Map<String, Object> map : parsedList) {
                categoryStates.add(new StatCategoryRateResponse.CategoryState(
                        (String) map.get("name"),
                        ((Number) map.get("ticket_count")).intValue()
                ));
            }

            responseList.add(new StatCategoryRateResponse(username, categoryStates));
        }

        return responseList;
    }

    private List<Map<String, Object>> parseJsonArray(String jsonArray) {
        // JSON을 파싱하여 List<Map<String, Object>> 형태로 변환 (Jackson 또는 Gson 사용)
        return List.of(); // JSON 파싱 로직을 구현해야 함
    }
}
