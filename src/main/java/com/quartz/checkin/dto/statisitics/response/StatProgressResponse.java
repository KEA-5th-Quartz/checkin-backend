package com.quartz.checkin.dto.statisitics.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

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