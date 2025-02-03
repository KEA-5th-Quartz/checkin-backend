package com.quartz.checkin.dto.statisitics.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class StatCategoryRateResponse {
    // Getters and Setters
    private String username;                // 담당자 이름
    private List<StatCategoryCountResponse> state;  // 카테고리별 티켓 수

    // 생성자
    public StatCategoryRateResponse(String username, List<StatCategoryCountResponse> state) {
        this.username = username;
        this.state = state;
    }

}