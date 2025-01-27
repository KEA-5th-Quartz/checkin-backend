package com.quartz.checkin.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagerTicketListResponse {
    private int page;
    private int size;
    private int totalPages;
    private int totalTickets;
    private List<ManagerTicketSummaryResponse> tickets;
}