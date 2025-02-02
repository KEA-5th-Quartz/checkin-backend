package com.quartz.checkin.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class StatCategoryRateResponse {
    // Getters and Setters
    private String username;                // 담당자 이름
    private List<StatCategoryCount> state;  // 카테고리별 티켓 수

    // 생성자
    public StatCategoryRateResponse(String username, List<StatCategoryCount> state) {
        this.username = username;
        this.state = state;
    }

}