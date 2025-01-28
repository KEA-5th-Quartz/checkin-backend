package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.converter.TicketResponseConverter;
import com.quartz.checkin.dto.request.TicketCreateRequest;
import com.quartz.checkin.dto.response.*;
import com.quartz.checkin.entity.*;
import com.quartz.checkin.repository.*;
import java.io.IOException;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TicketCudServiceImpl implements TicketCudService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final S3UploadService s3UploadService;

    @Transactional
    @Override
    public TicketCreateResponse createTicket(Long memberId, TicketCreateRequest request) {
        // 사용자 조회
        Member user = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));

        // 1차 카테고리 조회 (parent_id가 NULL인 경우)
        Category firstCategory = categoryRepository.findByNameAndParentIsNull(request.getFirstCategory())
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST));

        // 2차 카테고리 조회 (해당 1차 카테고리의 자식 카테고리)
        Category secondCategory = categoryRepository.findByNameAndParent(request.getSecondCategory(), firstCategory)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_SECOND));
        // 티켓 생성 및 저장
        Ticket ticket = Ticket.builder()
                .user(user)
                .firstCategory(firstCategory)
                .secondCategory(secondCategory)
                .title(request.getTitle())
                .content(request.getContent())
                .status(Status.OPEN)
                .dueDate(request.getDueDate())
                .build();
        ticketRepository.save(ticket);

        return new TicketCreateResponse(ticket.getId());
    }

    @Transactional
    @Override
    public TicketAttachmentResponse uploadAttachment(Long ticketId, MultipartFile file) throws IOException {
        // 티켓 존재 여부 확인
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));

        // S3에 파일 업로드
        String fileUrl = s3UploadService.uploadFile(file, "attachments");

        // 첨부파일 엔티티 저장
        TicketAttachment attachment = ticketAttachmentRepository.save(TicketAttachment.builder()
                .ticket(ticket)
                .url(fileUrl)
                .build());

        return TicketAttachmentResponse.from(attachment);
    }
}
