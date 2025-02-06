package com.quartz.checkin.service;

import com.quartz.checkin.dto.ticket.request.PriorityUpdateRequest;
import com.quartz.checkin.dto.ticket.request.TicketCreateRequest;
import com.quartz.checkin.dto.ticket.request.TicketUpdateRequest;
import com.quartz.checkin.dto.ticket.response.SoftDeletedTicketResponse;
import com.quartz.checkin.dto.ticket.response.TicketCreateResponse;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TicketCudService {
    TicketCreateResponse createTicket(Long memberId, TicketCreateRequest request);
    void updatePriority(Long memberId, Long ticketId, PriorityUpdateRequest request);
    void updateTicket(Long memberId, TicketUpdateRequest request, Long ticketId);
    void deleteTickets(Long memberId, List<Long> ticketIds);

    SoftDeletedTicketResponse getSoftDeletedTickets(Pageable pageable);

    void permanentlyDeleteTickets(Long memberId, @NotEmpty(message = "삭제할 티켓 ID 목록은 비어 있을 수 없습니다.") List<Long> ticketIds);
}
