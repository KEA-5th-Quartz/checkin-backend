package com.quartz.checkin.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TicketStatusChangedEvent {
    private final Long ticketId;
    private String customId;
    private Long agitId;
    private final int status;
}