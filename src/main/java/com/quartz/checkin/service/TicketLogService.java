package com.quartz.checkin.service;

import com.quartz.checkin.dto.category.request.FirstCategoryUpdateRequest;
import com.quartz.checkin.dto.category.request.SecondCategoryUpdateRequest;
import com.quartz.checkin.dto.ticket.response.TicketLogResponse;

public interface TicketLogService {
    TicketLogResponse assignManager(Long memberId, Long ticketId);
    TicketLogResponse closeTicket(Long memberId, Long ticketId);
    TicketLogResponse updateFirstCategory(Long memberId, Long ticketId, FirstCategoryUpdateRequest request);
    TicketLogResponse updateSecondCategory(Long memberId, Long ticketId, Long firstCategoryId, SecondCategoryUpdateRequest request);
    TicketLogResponse reassignManager(Long memberId, Long ticketId, String newManagerUsername);
}
