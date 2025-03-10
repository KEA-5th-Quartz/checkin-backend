package com.quartz.checkin.service;

import com.quartz.checkin.common.AttachmentUtils;
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

    private final AttachmentUtils attachmentUtils;
    private final AttachmentService attachmentService;
    private final AttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;
    private final CategoryServiceImpl categoryService;
    private final MemberService memberService;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public TicketCreateResponse createTicket(Long memberId, TicketCreateRequest request) {

        Member member = memberService.getMemberByIdOrThrow(memberId);
        Category firstCategory = categoryService.getFirstCategoryOrThrow(request.getFirstCategory());
        Category secondCategory = categoryService.getSecondCategoryOrThrow(request.getSecondCategory(), firstCategory);

        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("MMdd"));
        String firstCategoryAlias = firstCategory.getAlias();
        String secondCategoryAlias = secondCategory.getAlias();
        String prefix = datePart + firstCategoryAlias + "-" + secondCategoryAlias;

        int lastCustomId = ticketRepository.findLastCustomIdByDate(datePart);
        int lastTicketNumber = lastCustomId == 0 ? 1 : lastCustomId + 1;
        String newCustomId = prefix + String.format("%03d", lastTicketNumber);

        List<Long> attachmentIds = request.getAttachmentIds();
        List<Attachment> attachments = attachmentRepository.findAllById(attachmentIds);

        if (ticketRepository.existsByCustomId(newCustomId)) {
            log.error("중복된 customId가 감지되었습니다: {}", newCustomId);
            throw new ApiException(ErrorCode.DUPLICATE_TICKET_ID);
        }

        if (attachmentIds.size() > 3) {
            log.error("첨부파일은 최대 3개까지 등록 가능합니다.");
            throw new ApiException(ErrorCode.ATTACHMENT_LIMIT_EXCEEDED);
        }

        if (attachments.size() != attachmentIds.size()) {
            log.error("존재하지 않는 첨부파일이 포함되어 있습니다.");
            throw new ApiException(ErrorCode.ATTACHMENT_NOT_FOUND);
        }

        Ticket ticket = Ticket.builder()
                .customId(newCustomId)
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

        List<TicketAttachment> ticketAttachments = attachments.stream()
                .map(attachment -> new TicketAttachment(savedTicket, attachment))
                .toList();

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
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));

        Member member = memberService.getMemberByIdOrThrow(memberId);

        if (!ticket.getUser().getId().equals(member.getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        Category firstCategory = categoryService.getFirstCategoryOrThrow(request.getFirstCategory());
        Category secondCategory = categoryService.getSecondCategoryOrThrow(request.getSecondCategory(), firstCategory);

        ticket.updateTitle(request.getTitle());
        ticket.updateContent(request.getContent());
        ticket.updateCategories(firstCategory, secondCategory);
        ticket.updateDueDate(request.getDueDate());

        attachmentUtils.handleTicketAttachments(ticket, request.getAttachmentIds() != null ? request.getAttachmentIds() : Collections.emptyList());

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
    public void tempDeleteTickets(Long memberId, List<Long> ticketIds) {
        Member member = memberService.getMemberByIdOrThrow(memberId);

        List<Ticket> tickets = ticketRepository.findAllById(ticketIds);

        if (tickets.size() != ticketIds.size()) {
            throw new ApiException(ErrorCode.TICKET_NOT_FOUND);
        }

        List<Long> inProgressTickets = tickets.stream()
                .filter(ticket -> ticket.getStatus() == Status.IN_PROGRESS)
                .map(Ticket::getId)
                .toList();

        if (!inProgressTickets.isEmpty()) {
            throw new ApiException(ErrorCode.TICKET_ALREADY_ASSIGNED);
        }

        for (Ticket ticket : tickets) {
            if (!ticket.getUser().getId().equals(member.getId())) {
                throw new ApiException(ErrorCode.FORBIDDEN);
            }
        }

        List<Long> attachmentIds = ticketAttachmentRepository.findAttachmentIdsByTicketIds(ticketIds);

        if (!attachmentIds.isEmpty()) {
            ticketAttachmentRepository.deleteAllByTicketIds(ticketIds);
            attachmentService.deleteAttachments(attachmentIds);
        }

        List<Long> agitIdsToDelete = new ArrayList<>();

        for (Ticket ticket : tickets) {
            if (ticket.getAgitId() != null && ticket.getStatus() == Status.OPEN) {
                agitIdsToDelete.add(ticket.getAgitId());
                ticket.unlinkFromAgit();
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
        Member manager = memberService.getMemberByIdOrThrow(memberId);
        Ticket ticket = getValidTicket(ticketId);
        validateTicketManager(ticket, manager);

        ticket.updatePriority(request.getPriority());
        ticketRepository.save(ticket);
    }

    private Ticket getValidTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));
    }

    private void validateTicketManager(Ticket ticket, Member manager) {
        if (ticket.getManager() == null || !ticket.getManager().getId().equals(manager.getId())) {
            throw new ApiException(ErrorCode.INVALID_TICKET_MANAGER);
        }
    }
}
