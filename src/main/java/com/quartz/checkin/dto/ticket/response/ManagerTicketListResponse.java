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
    private int totalElements;
    private int dueTodayCount;
    private int openTicketCount;
    private int inProgressTicketCount;
    private int closedTicketCount;
    private String progressExpression;
    private List<ManagerTicketSummaryResponse> tickets;
}