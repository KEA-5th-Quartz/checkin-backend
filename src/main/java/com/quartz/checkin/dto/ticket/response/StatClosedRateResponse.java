package com.quartz.checkin.dto.response;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class StatClosedRateResponse {
    // Getters and Setters
    private double closed_rate; // 완료율 (예: 50.0)

    // 기본 생성자
    public StatClosedRateResponse() {}

    // 모든 필드를 포함한 생성자
    public StatClosedRateResponse(double closed_rate) {
        this.closed_rate = closed_rate;
    }

}