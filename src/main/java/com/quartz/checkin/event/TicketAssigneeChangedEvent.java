package com.quartz.checkin.event;

import java.util.List;
import lombok.Data;
import lombok.Getter;

@Getter
@Data
public class TicketAssigneeChangedEvent {

    private final Long agitId;
    private final Long managerId;
    private final Long assigneeId;
    private final Long ticketId;
    private final List<String> assigneesForInProgress;

    public TicketAssigneeChangedEvent(Long agitId, Long managerId, Long assigneeId, Long ticketId, List<String> assigneesForInProgress) {
        this.agitId = agitId;
        this.managerId = managerId;
        this.assigneeId = assigneeId;
        this.ticketId = ticketId;
        this.assigneesForInProgress = assigneesForInProgress;
    }
}