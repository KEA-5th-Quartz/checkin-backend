package com.quartz.checkin.service;

import com.quartz.checkin.dto.request.PriorityUpdateRequest;
import com.quartz.checkin.dto.request.TicketCreateRequest;
import com.quartz.checkin.dto.response.TicketCreateResponse;

import java.io.IOException;

public interface TicketCudService {
    TicketCreateResponse createTicket(Long memberId, TicketCreateRequest request) throws IOException;
    void updatePriority(Long memberId, Long ticketId, PriorityUpdateRequest request);
}
