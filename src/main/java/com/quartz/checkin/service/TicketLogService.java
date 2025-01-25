package com.quartz.checkin.service;

import com.quartz.checkin.dto.request.CategoryUpdateRequest;
import com.quartz.checkin.dto.response.TicketLogResponse;

public interface TicketLogService {
    TicketLogResponse assignManager(Long memberId, Long ticketId);
    TicketLogResponse closeTicket(Long memberId, Long ticketId);
    TicketLogResponse updateCategory(Long memberId, Long ticketId, CategoryUpdateRequest request);
    TicketLogResponse reassignManager(Long memberId, Long ticketId, String newManagerUsername);
}
