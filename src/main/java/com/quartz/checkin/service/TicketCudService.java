package com.quartz.checkin.service;

import com.quartz.checkin.dto.request.TicketCreateRequest;
import com.quartz.checkin.dto.response.TicketAttachmentResponse;
import com.quartz.checkin.dto.response.TicketCreateResponse;

import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface TicketCudService {
    TicketCreateResponse createTicket(Long memberId, TicketCreateRequest request, List<MultipartFile> files) throws IOException;
    TicketAttachmentResponse uploadAttachment(Long ticketId, MultipartFile file) throws IOException;
}
