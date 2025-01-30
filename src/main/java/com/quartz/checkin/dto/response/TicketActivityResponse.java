package com.quartz.checkin.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class TicketActivityResponse {
    private Long ticketId;
    private List<ActivityResponse> activities;
}

