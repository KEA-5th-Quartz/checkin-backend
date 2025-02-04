package com.quartz.checkin.dto.statisitics.response;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class StatTotalProgressResponse {
    // Getter, Setter
    private int overdue;
    private List<StatusCount> state;

    // 기본 생성자
    public StatTotalProgressResponse() {
        this.overdue = 0;
        this.state = new ArrayList<>();
    }

    // 매개변수가 있는 생성자
    public StatTotalProgressResponse(int overdue, List<StatusCount> state) {
        this.overdue = overdue;
        this.state = state;
    }

    // StatusCount 내부 클래스
    @Setter
    @Getter
    public static class StatusCount {
        // Getter, Setter
        private String status;
        private int ticket_count;

        // 기본 생성자
        public StatusCount() {
        }

        // 매개변수가 있는 생성자
        public StatusCount(String status, int ticket_count) {
            this.status = status;
            this.ticket_count = ticket_count;
        }

    }
}