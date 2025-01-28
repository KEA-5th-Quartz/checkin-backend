package com.quartz.checkin.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TicketAssigneeChangedEvent {
    private Long ticketId;
    private Long userId;
    private Long oldAssigneeId;
    private Long newAssigneeId;
}