package com.quartz.checkin.event;

import java.util.List;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TicketDeletedEvent {
    private final String ticketId;
    private final List<Long> agitIds;

    public TicketDeletedEvent(String ticketId, List<Long> agitIds) {
        this.ticketId = ticketId;
        this.agitIds = agitIds;
    }

    public TicketDeletedEvent(String ticketId, Long agitId) {
        this.ticketId = ticketId;
        this.agitIds = List.of(agitId);
    }
}

