package com.quartz.checkin.event;

import java.util.List;
import lombok.Getter;

@Getter
public class TicketDeletedEvent {
    private final Long ticketId;
    private final List<Long> agitIds;

    public TicketDeletedEvent(Long ticketId, List<Long> agitIds) {
        this.ticketId = ticketId;
        this.agitIds = agitIds;
    }

    public TicketDeletedEvent(Long ticketId, Long agitId) {
        this.ticketId = ticketId;
        this.agitIds = List.of(agitId);
    }
}

