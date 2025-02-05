package com.quartz.checkin.dto.ticket.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TicketProgressResponse {
    private long dueTodayCount;
    private long openTicketCount;
    private long inProgressTicketCount;
    private long closedTicketCount;
    private String progressExpression;
}