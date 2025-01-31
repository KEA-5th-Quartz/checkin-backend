package com.quartz.checkin.service;

import com.quartz.checkin.dto.request.PriorityUpdateRequest;
import com.quartz.checkin.dto.request.TicketCreateRequest;
import com.quartz.checkin.dto.response.TicketCreateResponse;

public interface TicketCudService {
    TicketCreateResponse createTicket(Long memberId, TicketCreateRequest request) ;
    void updatePriority(Long memberId, Long ticketId, PriorityUpdateRequest request);
}
