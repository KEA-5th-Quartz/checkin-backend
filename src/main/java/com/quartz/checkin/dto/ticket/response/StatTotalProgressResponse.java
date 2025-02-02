package com.quartz.checkin.dto.response;

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
}