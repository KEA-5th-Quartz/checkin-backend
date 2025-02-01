package com.quartz.checkin.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Setter
@Getter
public class StatProgressResponse {
    // Getters and Setters
    private String username;
    private List<StatusRate> state;

    // 기본 생성자
    public StatProgressResponse() {}

    // 모든 필드를 포함한 생성자
    public StatProgressResponse(String username, List<StatusRate> state) {
        this.username = username;
        this.state = state;
    }

    // 상태 비율을 나타내는 내부 클래스
    @Setter
    @Getter
    public static class StatusRate {
        // Getters and Setters
        private String status; // "In Progress", "Closed"
        private double rate;   // 비율 (예: 60.0)

        // 기본 생성자
        public StatusRate() {}

        // 모든 필드를 포함한 생성자
        public StatusRate(String status, double rate) {
            this.status = status;
            this.rate = rate;
        }

    }
}