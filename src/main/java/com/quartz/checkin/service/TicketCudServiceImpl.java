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
import com.quartz.checkin.event.TicketCreatedEvent;
import com.quartz.checkin.event.TicketDeletedEvent;
import com.quartz.checkin.repository.AttachmentRepository;
import com.quartz.checkin.repository.TicketAttachmentRepository;
import com.quartz.checkin.repository.TicketRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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

    private final AttachmentService attachmentService;
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

        // 날짜 기반 prefix 생성 (MMDD + 1차 카테고리 alias + "-" + 2차 카테고리 alias)
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("MMdd"));
        String firstCategoryAlias = firstCategory.getAlias();
        String secondCategoryAlias = secondCategory.getAlias();
        String prefix = datePart + firstCategoryAlias + "-" + secondCategoryAlias;

        // 기존 티켓 중 오늘 날짜에 해당하는 마지막 티켓 ID 조회
        String lastCustomId = ticketRepository.findLastTicketId(prefix);

        int lastTicketNumber = 1; // 기본값은 1
        if (lastCustomId != null && lastCustomId.startsWith(prefix)) {
            try {
                lastTicketNumber = Integer.parseInt(lastCustomId.substring(lastCustomId.length() - 3)) + 1;
            } catch (NumberFormatException e) {
                log.error("티켓 번호 생성 중 숫자 변환 오류 발생: {}", lastCustomId, e);
                throw new ApiException(ErrorCode.INVALID_DATA);
            }
        }

        // 새 티켓 ID 생성
        String newCustomId = prefix + String.format("%03d", lastTicketNumber);

        // 첨부파일 검증
        List<Long> attachmentIds = request.getAttachmentIds();
        List<Attachment> attachments = attachmentRepository.findAllById(attachmentIds);

        if (attachments.size() != attachmentIds.size()) {
            log.error("존재하지 않는 첨부파일이 포함되어 있습니다.");
            throw new ApiException(ErrorCode.INVALID_TEMPLATE_ATTACHMENT_IDS);
        }

        // 티켓 생성 및 저장
        Ticket ticket = Ticket.builder()
                .customId(newCustomId) // 수정된 티켓 ID 적용
                .user(member)
                .firstCategory(firstCategory)
                .secondCategory(secondCategory)
                .title(request.getTitle())
                .content(request.getContent())
                .priority(Priority.UNDEFINED)
                .status(Status.OPEN)
                .dueDate(request.getDueDate())
                .agitId(null)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);

        List<TicketAttachment> ticketAttachments = new ArrayList<>();
        for (Attachment attachment : attachments) {
            ticketAttachments.add(new TicketAttachment(savedTicket, attachment));
        }

        ticketAttachmentRepository.saveAll(ticketAttachments);

        eventPublisher.publishEvent(new TicketCreatedEvent(
                savedTicket.getId(),
                savedTicket.getCustomId(),
                savedTicket.getUser().getId(),
                savedTicket.getTitle(),
                savedTicket.getContent(),
                List.of(member.getUsername())
        ));

        return new TicketCreateResponse(savedTicket.getId());
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

        // 기존 정보 저장
        Category oldFirstCategory = ticket.getFirstCategory();
        Category oldSecondCategory = ticket.getSecondCategory();

        // 기존 createdAt을 기준으로 날짜 유지
        String datePart = ticket.getCreatedAt().format(DateTimeFormatter.ofPattern("MMdd"));

        // 1차 카테고리 또는 2차 카테고리가 변경되었을 경우만 customId 변경
        if (!oldFirstCategory.equals(firstCategory) || !oldSecondCategory.equals(secondCategory)) {
            String firstCategoryAlias = firstCategory.getAlias();
            String secondCategoryAlias = secondCategory.getAlias();
            String prefix = datePart + firstCategoryAlias + "-" + secondCategoryAlias;

            // 기존 customId에서 숫자 부분만 추출 (마지막 3자리)
            String oldCustomId = ticket.getCustomId();
            String numberPart = oldCustomId.substring(oldCustomId.length() - 3); // 001, 002 같은 숫자 부분 유지

            // 새로운 customId 생성 (앞부분만 변경하고, 숫자는 유지)
            String newCustomId = prefix + numberPart;
            ticket.updateCustomId(newCustomId);
        }


        // 첨부파일 검증 및 변경 사항 반영 (null 체크 추가)
        List<Long> newAttachmentIds = request.getAttachmentIds() != null ? request.getAttachmentIds() : Collections.emptyList();
        List<Attachment> newAttachments = newAttachmentIds.isEmpty() ? Collections.emptyList() : attachmentRepository.findAllById(newAttachmentIds);
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
                attachmentService.deleteAttachments(finalAttachmentsToDelete);
            }
        }
        // 티켓 필드 업데이트
        ticket.updateTitle(request.getTitle());
        ticket.updateContent(request.getContent());
        ticket.updateCategories(firstCategory, secondCategory);
        ticket.updateDueDate(request.getDueDate());

        Ticket savedTicket = ticketRepository.save(ticket);

        if (ticket.getAgitId() != null) {
            eventPublisher.publishEvent(new TicketDeletedEvent(ticket.getId(), ticket.getAgitId()));
            ticket.unlinkFromAgit();
        }

        eventPublisher.publishEvent(new TicketCreatedEvent(
                savedTicket.getId(),
                savedTicket.getCustomId(),
                savedTicket.getUser().getId(),
                savedTicket.getTitle(),
                savedTicket.getContent(),
                List.of(member.getUsername())
        ));

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

        // 진행 중인(IN_PROGRESS) 티켓이 포함되어 있는지 확인
        List<Long> inProgressTickets = tickets.stream()
                .filter(ticket -> ticket.getStatus() == Status.IN_PROGRESS)
                .map(Ticket::getId)
                .toList();

        if (!inProgressTickets.isEmpty()) {
            throw new ApiException(ErrorCode.TICKET_ALREADY_ASSIGNED);
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
            ticketAttachmentRepository.deleteAllByTicketIds(ticketIds); // 연결 데이터 삭제
            attachmentService.deleteAttachments(attachmentIds); // 첨부파일 삭제
        }

        List<Long> agitIdsToDelete = new ArrayList<>();

        for (Ticket ticket : tickets) {
            if (ticket.getAgitId() != null && ticket.getStatus() == Status.OPEN) {
                agitIdsToDelete.add(ticket.getAgitId()); // 나중에 이벤트로 삭제 처리
                ticket.unlinkFromAgit(); // 기존 agitId 제거
            }
            tickets.forEach(Ticket::softDelete);
        }

        List<Long> ticketIdsToDelete = tickets.stream()
                .map(Ticket::getId)
                .toList();

        if (!agitIdsToDelete.isEmpty()) {
            for (Long ticketId : ticketIdsToDelete) {
                eventPublisher.publishEvent(new TicketDeletedEvent(ticketId, agitIdsToDelete));
            }
        }

        ticketRepository.saveAll(tickets);
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
        ticketRepository.save(ticket);
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
