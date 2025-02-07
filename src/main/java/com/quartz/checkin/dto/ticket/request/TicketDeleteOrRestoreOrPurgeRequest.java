package com.quartz.checkin.dto.ticket.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;

@Getter
public class TicketDeleteOrRestoreOrPurgeRequest {

    @NotNull
    private List<Long> ticketIds;
}

