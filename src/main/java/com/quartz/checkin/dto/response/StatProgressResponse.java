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

    // 생성자
    public StatProgressResponse(String username, List<StatusRate> state) {
        this.username = username;
        this.state = state;
    }

    @Setter
    @Getter
    public static class StatusRate {
        // Getters and Setters
        private String status;
        private double rate;

        // 생성자
        public StatusRate(String status, double rate) {
            this.status = status;
            this.rate = rate;
        }

    }
}