package com.quartz.checkin.service;

import com.quartz.checkin.dto.request.TicketCreateRequest;
import com.quartz.checkin.dto.response.TicketCreateResponse;
import com.quartz.checkin.dto.response.TicketDetailResponse;

public interface TicketCrudService {
    TicketCreateResponse createTicket(Long memberId, TicketCreateRequest request);
    TicketDetailResponse getTicketDetail(Long memberId, Long ticketId);
}
