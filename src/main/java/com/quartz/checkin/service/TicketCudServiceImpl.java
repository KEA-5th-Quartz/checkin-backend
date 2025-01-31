package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.config.S3Config;
import com.quartz.checkin.dto.request.PriorityUpdateRequest;
import com.quartz.checkin.dto.request.TicketCreateRequest;
import com.quartz.checkin.dto.response.TicketAttachmentResponse;
import com.quartz.checkin.dto.response.TicketCreateResponse;
import com.quartz.checkin.entity.Category;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Priority;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.entity.TicketAttachment;
import com.quartz.checkin.repository.*;
import java.io.IOException;
import java.util.List;
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
    public TicketCreateResponse createTicket(Long memberId, TicketCreateRequest request, List<MultipartFile> files) throws IOException {
        // 사용자 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));

        // 1차 카테고리 조회 (parent_id가 NULL인 경우)
        Category firstCategory = categoryRepository.findByNameAndParentIsNull(request.getFirstCategory())
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST));

        // 2차 카테고리 조회 (해당 1차 카테고리의 자식 카테고리)
        Category secondCategory = categoryRepository.findByNameAndParent(request.getSecondCategory(), firstCategory)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_SECOND));
        // 티켓 생성 및 저장
        Ticket ticket = Ticket.builder()
                .user(member)
                .firstCategory(firstCategory)
                .secondCategory(secondCategory)
                .title(request.getTitle())
                .content(request.getContent())
                .priority(Priority.UNDEFINED)
                .status(Status.OPEN)
                .dueDate(request.getDueDate())
                .build();
        ticketRepository.save(ticket);

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String fileUrl = s3UploadService.uploadFile(file, S3Config.TICKET_DIR);
                    TicketAttachment attachment = new TicketAttachment(ticket, fileUrl);
                    ticketAttachmentRepository.save(attachment);
                }
            }
        }
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

    @Transactional
    public void updatePriority(Long memberId, Long ticketId, PriorityUpdateRequest request) {
        // 담당자 검증
        Member manager = getValidMember(memberId);

        // 티켓 검증 및 중요도 업데이트
        Ticket ticket = getValidTicket(ticketId);

        // 담당자 권한 검증
        validateTicketManager(ticket, manager);

        // 중요도 변경
        ticket.updatePriority(request.getPriority());
        ticketRepository.save(ticket); // 안전하게 변경 사항 저장
    }

    private Ticket getValidTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));
    }

    private Member getValidMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void validateTicketManager(Ticket ticket, Member manager) {
        // 담당자가 본인이 맞는지 검증
        if (ticket.getManager() == null || !ticket.getManager().getId().equals(manager.getId())) {
            throw new ApiException(ErrorCode.INVALID_TICKET_MANAGER);
        }
    }
}
