package com.quartz.checkin.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTicketListResponse {
    private int page;
    private int size;
    private int totalPages;
    private int totalTickets;
    private List<UserTicketSummaryResponse> tickets;
}