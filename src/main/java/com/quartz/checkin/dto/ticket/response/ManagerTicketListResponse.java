package com.quartz.checkin.dto.ticket.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagerTicketListResponse {
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;
    private List<ManagerTicketSummaryResponse> tickets;
}