package com.quartz.checkin.dto.response;

import lombok.*;

import java.util.List;

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