package com.quartz.checkin.service;

import com.quartz.checkin.dto.request.TicketCreateRequest;
import com.quartz.checkin.dto.response.TicketCreateResponse;

public interface TicketService {
    TicketCreateResponse createTicket(Long memberId, TicketCreateRequest request);
}
