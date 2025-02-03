package com.quartz.checkin.dto.statisitics.response;

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
        private int ticket_count;   // 비율 (예: 60.0)

        // 기본 생성자
        public StatusRate() {}

        // 모든 필드를 포함한 생성자
        public StatusRate(String status, int ticket_count) {
            this.status = status;
            this.ticket_count = ticket_count;
        }

    }
}