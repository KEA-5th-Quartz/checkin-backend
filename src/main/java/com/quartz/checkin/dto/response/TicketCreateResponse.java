package com.quartz.checkin.dto.response;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TicketCreateResponse {
    private final Long ticketId;

    public TicketCreateResponse(Long ticketId) {
        this.ticketId = ticketId;
    }
}
