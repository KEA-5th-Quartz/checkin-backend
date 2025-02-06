package com.quartz.checkin.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TicketCreatedEvent {
    private Long ticketId;
    private Long userId;
    private Long agitId;
    private String title;
}

