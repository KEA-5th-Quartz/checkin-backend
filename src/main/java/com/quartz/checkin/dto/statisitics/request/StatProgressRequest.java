package com.quartz.checkin.dto.statisitics.request;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class StatProgressRequest {
    // Getters and Setters
    private String type; // "WEEK", "MONTH", "QUARTER"

    // 기본 생성자
    public StatProgressRequest() {}

    // 모든 필드를 포함한 생성자
    public StatProgressRequest(String type) {
        this.type = type;
    }

}