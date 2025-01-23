package com.quartz.checkin.service;

import com.quartz.checkin.dto.request.TicketRequest;
import com.quartz.checkin.dto.response.TicketResponse;

public interface TicketService {
    TicketResponse createTicket(Long memberId, TicketRequest request);
}
