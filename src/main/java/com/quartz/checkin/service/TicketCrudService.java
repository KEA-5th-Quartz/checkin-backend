package com.quartz.checkin.service;

import com.quartz.checkin.dto.request.TicketCreateRequest;
import com.quartz.checkin.dto.response.TicketCreateResponse;
import com.quartz.checkin.dto.response.TicketDetailResponse;
import com.quartz.checkin.dto.response.TicketTotalListResponse;
import com.quartz.checkin.entity.Priority;
import com.quartz.checkin.entity.Status;

public interface TicketCrudService {
    TicketCreateResponse createTicket(Long memberId, TicketCreateRequest request);
    TicketDetailResponse getTicketDetail(Long memberId, Long ticketId);
    TicketTotalListResponse getManagerTickets(Long memberId, Status status, String username, String category, Priority priority, int page, int size);
}
