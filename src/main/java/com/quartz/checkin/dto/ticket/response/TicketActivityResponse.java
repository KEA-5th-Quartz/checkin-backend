package com.quartz.checkin.dto.ticket.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TicketActivityResponse {
    private Long id;
    private List<ActivityResponse> activities;
}

