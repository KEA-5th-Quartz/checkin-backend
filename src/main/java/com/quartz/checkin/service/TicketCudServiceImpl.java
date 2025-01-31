package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.dto.request.PriorityUpdateRequest;
import com.quartz.checkin.dto.request.TicketCreateRequest;
import com.quartz.checkin.dto.response.TicketCreateResponse;
import com.quartz.checkin.entity.Attachment;
import com.quartz.checkin.entity.Category;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Priority;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.entity.TicketAttachment;
import com.quartz.checkin.repository.AttachmentRepository;
import com.quartz.checkin.repository.TicketAttachmentRepository;
import com.quartz.checkin.repository.TicketRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketCudServiceImpl implements TicketCudService {

    private final AttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;
    private final CategoryService categoryService;
    private final MemberService memberService;
    private final TicketAttachmentRepository ticketAttachmentRepository;


    @Transactional
    @Override
    public TicketCreateResponse createTicket(Long memberId, TicketCreateRequest request) throws IOException {
        // 사용자 조회
        Member member = memberService.getMemberByIdOrThrow(memberId);

        // 1차 카테고리 조회 (parent_id가 NULL인 경우)
        Category firstCategory = categoryService.getFirstCategoryOrThrow(request.getFirstCategory());

        // 2차 카테고리 조회 (해당 1차 카테고리의 자식 카테고리)
        Category secondCategory = categoryService.getSecondCategoryOrThrow(request.getSecondCategory(), firstCategory);

        // 첨부파일 검증
        List<Long> attachmentIds = request.getAttachmentIds();
        List<Attachment> attachments = attachmentRepository.findAllById(attachmentIds);

        if (attachments.size() != attachmentIds.size()) {
            log.error("존재하지 않는 첨부파일이 포함되어 있습니다.");
            throw new ApiException(ErrorCode.INVALID_TEMPLATE_ATTACHMENT_IDS);
        }

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
        Ticket savedTicket = ticketRepository.save(ticket);

        List<TicketAttachment> ticketAttachments = new ArrayList<>();
        for (Attachment attachment : attachments) {
            ticketAttachments.add(new TicketAttachment(ticket, attachment));
        }

        ticketAttachmentRepository.saveAll(ticketAttachments);

        return new TicketCreateResponse(ticket.getId());
    }

    @Transactional
    public void updatePriority(Long memberId, Long ticketId, PriorityUpdateRequest request) {
        // 담당자 검증
        Member manager = memberService.getMemberByIdOrThrow(memberId);

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


    private void validateTicketManager(Ticket ticket, Member manager) {
        // 담당자가 본인이 맞는지 검증
        if (ticket.getManager() == null || !ticket.getManager().getId().equals(manager.getId())) {
            throw new ApiException(ErrorCode.INVALID_TICKET_MANAGER);
        }
    }
}
