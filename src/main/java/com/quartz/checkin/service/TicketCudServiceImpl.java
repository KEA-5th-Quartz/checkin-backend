package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.ticket.request.PriorityUpdateRequest;
import com.quartz.checkin.dto.ticket.request.TicketCreateRequest;
import com.quartz.checkin.dto.ticket.request.TicketUpdateRequest;
import com.quartz.checkin.dto.ticket.response.TicketCreateResponse;
import com.quartz.checkin.entity.Attachment;
import com.quartz.checkin.entity.Category;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Priority;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.entity.TicketAttachment;
import com.quartz.checkin.event.NotificationEvent;
import com.quartz.checkin.repository.AttachmentRepository;
import com.quartz.checkin.repository.TicketAttachmentRepository;
import com.quartz.checkin.repository.TicketRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TicketCudServiceImpl implements TicketCudService {

    private final AttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;
    private final CategoryServiceImpl categoryService;
    private final MemberService memberService;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final WebhookService webhookService;
    private final ApplicationEventPublisher eventPublisher;


    @Override
    public TicketCreateResponse createTicket(Long memberId, TicketCreateRequest request) {
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

        // 요청 보낸 사용자를 담당자로 설정하여 웹훅으로 게시물 생성
        Long agitId = webhookService.createAgitPost(
                request.getTitle(),
                request.getContent(),
                List.of(member.getUsername()) // 요청한 사용자 지정
        );

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
                .agitId(agitId)
                .build();
        Ticket savedTicket = ticketRepository.save(ticket);

        List<TicketAttachment> ticketAttachments = new ArrayList<>();
        for (Attachment attachment : attachments) {
            ticketAttachments.add(new TicketAttachment(savedTicket, attachment));
        }

        ticketAttachmentRepository.saveAll(ticketAttachments);

        // 티켓 생성 이벤트 발행
        eventPublisher.publishEvent(new NotificationEvent(
                ticket.getId(), // relatedId
                "TICKET_CREATED", // type
                "ticket", // relatedTable
                ticket.getUser().getId(), // memberId
                ticket.getUser().getId(), // userId
                ticket.getManager() != null ? ticket.getManager().getId() : null,
                null,
                ticket.getAgitId(), // agitId
                "새로운 티켓이 생성되었습니다."
        ));


        return new TicketCreateResponse(ticket.getId());
    }

    @Override
    public void updateTicket(Long memberId, TicketUpdateRequest request, Long ticketId) {
        // 티켓 조회 및 존재 여부 검증
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));

        // 사용자 조회
        Member member = memberService.getMemberByIdOrThrow(memberId);

        // 본인이 생성한 티켓인지 검증
        if (!ticket.getUser().getId().equals(member.getId())) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }

        // 카테고리 검증 (1차 및 2차)
        Category firstCategory = categoryService.getFirstCategoryOrThrow(request.getFirstCategory());
        Category secondCategory = categoryService.getSecondCategoryOrThrow(request.getSecondCategory(), firstCategory);

        // 첨부파일 검증 및 변경 사항 반영
        List<Long> newAttachmentIds = request.getAttachmentIds();
        List<Attachment> newAttachments = attachmentRepository.findAllById(newAttachmentIds);
        checkInvalidAttachment(newAttachmentIds, newAttachments);

        // 현재 저장된 첨부파일 ID 조회
        List<Long> savedAttachmentIds = ticketAttachmentRepository.findByTicketId(ticketId)
                .stream()
                .map(ta -> ta.getAttachment().getId())
                .toList();

        // 추가해야 할 첨부파일 ID 확인
        List<Long> attachmentIdsToAdd = newAttachmentIds.stream()
                .filter(id -> !savedAttachmentIds.contains(id))
                .toList();

        // 제거해야 할 첨부파일 ID 확인
        List<Long> attachmentIdsToRemove = savedAttachmentIds.stream()
                .filter(id -> !newAttachmentIds.contains(id))
                .toList();

        // 추가해야 할 첨부파일 엔티티 생성
        List<Attachment> attachmentsToAdd = newAttachments.stream()
                .filter(a -> attachmentIdsToAdd.contains(a.getId()))
                .toList();

        List<TicketAttachment> newTicketAttachments = attachmentsToAdd.stream()
                .map(a -> new TicketAttachment(ticket, a))
                .toList();

        // 첨부파일 업데이트 (추가 및 삭제)
        ticketAttachmentRepository.saveAll(newTicketAttachments);

        if (!attachmentIdsToRemove.isEmpty()) {
            // 중간 테이블에서 삭제
            ticketAttachmentRepository.deleteByTicketAndAttachmentIds(ticket, attachmentIdsToRemove);

            // 삭제 대상 첨부파일이 다른 티켓에서도 사용되는지 확인
            List<Long> usedAttachmentIds = ticketAttachmentRepository.findAttachmentIdsInUse(attachmentIdsToRemove);
            List<Long> finalAttachmentsToDelete = attachmentIdsToRemove.stream()
                    .filter(id -> !usedAttachmentIds.contains(id)) // 다른 티켓에서 사용되지 않는 것만 필터링
                    .toList();

            if (!finalAttachmentsToDelete.isEmpty()) {
                // 사용되지 않는 첨부파일 삭제
                attachmentRepository.deleteAllByIdInBatch(finalAttachmentsToDelete);
            }
        }
        // 티켓 필드 업데이트
        ticket.updateTitle(request.getTitle());
        ticket.updateContent(request.getContent());
        ticket.updateCategories(firstCategory, secondCategory);
        ticket.updateDueDate(request.getDueDate());

        ticketRepository.save(ticket);
    }

    @Override
    public void deleteTickets(Long memberId, List<Long> ticketIds) {
        // 현재 사용자 조회
        Member member = memberService.getMemberByIdOrThrow(memberId);

        // 삭제할 티켓 조회
        List<Ticket> tickets = ticketRepository.findAllById(ticketIds);

        // 존재하지 않는 티켓이 있는 경우 예외 처리
        if (tickets.size() != ticketIds.size()) {
            throw new ApiException(ErrorCode.TICKET_NOT_FOUND);
        }

        // 사용자가 생성한 티켓인지 확인
        for (Ticket ticket : tickets) {
            if (!ticket.getUser().getId().equals(member.getId())) {
                throw new ApiException(ErrorCode.UNAUTHENTICATED);
            }
        }

        // 티켓에 연결된 첨부파일 ID 조회
        List<Long> attachmentIds = ticketAttachmentRepository.findAttachmentIdsByTicketIds(ticketIds);

        // 첨부파일 영구 삭제
        if (!attachmentIds.isEmpty()) {
            ticketAttachmentRepository.deleteAllByIdInBatch(ticketIds); // 연결 데이터 삭제
            attachmentRepository.deleteAllById(attachmentIds); // 첨부파일 삭제
        }

        // 티켓 소프트 삭제 (deleted_at 업데이트)
        ticketRepository.updateDeletedAtByIds(ticketIds, LocalDateTime.now());
    }

    @Override
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

    private void checkInvalidAttachment(List<Long> attachmentIds, List<Attachment> attachments) {
        if (attachmentIds.size() != attachments.size()) {
            log.error("존재하지 않는 첨부파일이 포함되어 있습니다.");
            throw new ApiException(ErrorCode.INVALID_TEMPLATE_ATTACHMENT_IDS);
        }
    }
}
