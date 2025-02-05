package com.quartz.checkin.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TicketStatusChangedEvent {
    private Long ticketId;
    private String oldStatus;
    private String newStatus;
    private Long userId;
}