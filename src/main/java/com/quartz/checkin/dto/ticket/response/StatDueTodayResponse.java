package com.quartz.checkin.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StatDueTodayResponse {
    // Getters and Setters
    private String username;
    private int ticketCount;

    // Constructor
    public StatDueTodayResponse(String username, int ticketCount) {
        this.username = username;
        this.ticketCount = ticketCount;
    }

}