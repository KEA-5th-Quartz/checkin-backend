package com.quartz.checkin.service;

import com.quartz.checkin.dto.ticket.response.TicketCreateResponse;
import com.quartz.checkin.dto.ticket.request.PriorityUpdateRequest;
import com.quartz.checkin.dto.ticket.request.TicketCreateRequest;

public interface TicketCudService {
    TicketCreateResponse createTicket(Long memberId, TicketCreateRequest request) ;
    void updatePriority(Long memberId, Long ticketId, PriorityUpdateRequest request);
}
