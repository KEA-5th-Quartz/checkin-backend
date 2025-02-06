package com.quartz.checkin.service;

import com.quartz.checkin.dto.ticket.request.PriorityUpdateRequest;
import com.quartz.checkin.dto.ticket.request.TicketCreateRequest;
import com.quartz.checkin.dto.ticket.request.TicketUpdateRequest;
import com.quartz.checkin.dto.ticket.response.TicketCreateResponse;
import java.util.List;

public interface TicketCudService {
    TicketCreateResponse createTicket(Long memberId, TicketCreateRequest request);
    void updatePriority(Long memberId, String ticketId, PriorityUpdateRequest request);
    void updateTicket(Long memberId, TicketUpdateRequest request, String ticketId);
    void deleteTickets(Long memberId, List<String> ticketIds);
}
