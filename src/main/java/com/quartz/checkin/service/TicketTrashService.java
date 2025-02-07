package com.quartz.checkin.service;

import com.quartz.checkin.dto.ticket.response.SoftDeletedTicketResponse;
import java.util.List;

public interface TicketTrashService {
    void restoreTickets(Long memberId, List<Long> ticketIds);
    void deleteTickets(Long memberId, List<Long> ticketIds);
    SoftDeletedTicketResponse getDeletedTickets(Long memberId, int page, int size);
}
