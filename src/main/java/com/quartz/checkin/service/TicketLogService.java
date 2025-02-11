package com.quartz.checkin.service;

import com.quartz.checkin.dto.category.request.FirstCategoryPatchRequest;
import com.quartz.checkin.dto.category.request.SecondCategoryPatchRequest;
import com.quartz.checkin.dto.ticket.response.TicketLogResponse;

public interface TicketLogService {
    TicketLogResponse closeTicket(Long memberId, Long ticketId);
    TicketLogResponse updateFirstCategory(Long memberId, Long ticketId, FirstCategoryPatchRequest request);
    TicketLogResponse updateSecondCategory(Long memberId, Long ticketId, Long firstCategoryId, SecondCategoryPatchRequest request);
    TicketLogResponse assignManager(Long memberId, Long ticketId, String managerUsername);
}
