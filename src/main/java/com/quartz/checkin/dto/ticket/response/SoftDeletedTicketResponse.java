package com.quartz.checkin.dto.ticket.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SoftDeletedTicketResponse {
    private int page;
    private int size;
    private int totalPages;
    private long totalTickets;
    private List<DeletedTicketDetail> tickets;
}
