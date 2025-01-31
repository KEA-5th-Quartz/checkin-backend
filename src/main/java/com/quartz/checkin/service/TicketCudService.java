package com.quartz.checkin.service;

import com.quartz.checkin.dto.ticket.response.TicketAttachmentResponse;
import com.quartz.checkin.dto.ticket.response.TicketCreateResponse;
import com.quartz.checkin.dto.ticket.request.PriorityUpdateRequest;
import com.quartz.checkin.dto.ticket.request.TicketCreateRequest;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface TicketCudService {
    TicketCreateResponse createTicket(Long memberId, TicketCreateRequest request, List<MultipartFile> files) throws IOException;
    TicketAttachmentResponse uploadAttachment(Long ticketId, MultipartFile file) throws IOException;
    void updatePriority(Long memberId, Long ticketId, PriorityUpdateRequest request);
}
