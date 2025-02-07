package com.quartz.checkin.service;

import com.quartz.checkin.dto.ticket.request.PriorityUpdateRequest;
import com.quartz.checkin.dto.ticket.request.TicketCreateRequest;
import com.quartz.checkin.dto.ticket.request.TicketUpdateRequest;
import com.quartz.checkin.dto.ticket.response.TicketCreateResponse;
import java.util.List;

public interface TicketCudService {
    TicketCreateResponse createTicket(Long memberId, TicketCreateRequest request);
    void updatePriority(Long memberId, Long ticketId, PriorityUpdateRequest request);
    void updateTicket(Long memberId, TicketUpdateRequest request, Long ticketId);
    void tempDeleteTickets(Long memberId, List<Long> ticketIds);
}
