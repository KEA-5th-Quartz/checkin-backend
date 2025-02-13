package com.quartz.checkin.dto.stat.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StatProgressResponse {

    private String userName;
    private List<StatusRate> state;

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusRate {

        private String status; // "In Progress", "Closed"
        private int ticketCount;

    }
}