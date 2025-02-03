package com.quartz.checkin.dto.statisitics.response;

/*
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StatTotalProgressResponse {
    private int deleted_count;
    private List<StatusCount> state;

    @Getter
    @Setter
    public static class StatusCount {
        private String status;
        private int ticket_count;
    }
}*/

import java.util.List;

public class StatTotalProgressResponse {
    private int deleted_count;
    private List<StatusCount> state;

    // 기본 생성자
    public StatTotalProgressResponse() {}

    // 생성자
    public StatTotalProgressResponse(int deleted_count, List<StatusCount> state) {
        this.deleted_count = deleted_count;
        this.state = state;
    }

    // Setter 메서드
    public void setDeleted_count(int deleted_count) {
        this.deleted_count = deleted_count;
    }

    public void setState(List<StatusCount> state) {
        this.state = state;
    }

    // Getter 메서드
    public int getDeleted_count() {
        return deleted_count;
    }

    public List<StatusCount> getState() {
        return state;
    }

    // StatusCount 내부 클래스
    public static class StatusCount {
        private String status;
        private int ticket_count;

        // 생성자, Getter, Setter 생략
    }
}