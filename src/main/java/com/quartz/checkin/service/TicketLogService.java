package com.quartz.checkin.service;

import com.quartz.checkin.dto.category.request.FirstCategoryUpdateRequest;
import com.quartz.checkin.dto.category.request.SecondCategoryUpdateRequest;
import com.quartz.checkin.dto.ticket.response.TicketLogResponse;

public interface TicketLogService {
    TicketLogResponse assignManager(Long memberId, String ticketId);
    TicketLogResponse closeTicket(Long memberId, String ticketId);
    TicketLogResponse updateFirstCategory(Long memberId, String ticketId, FirstCategoryUpdateRequest request);
    TicketLogResponse updateSecondCategory(Long memberId, String ticketId, Long firstCategoryId, SecondCategoryUpdateRequest request);
    TicketLogResponse reassignManager(Long memberId, String ticketId, String newManagerUsername);
}
