package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.category.request.FirstCategoryPatchRequest;
import com.quartz.checkin.dto.category.request.SecondCategoryPatchRequest;
import com.quartz.checkin.dto.ticket.response.TicketLogResponse;
import com.quartz.checkin.entity.Category;
import com.quartz.checkin.entity.LogType;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Priority;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.entity.TicketLog;
import com.quartz.checkin.event.TicketAssigneeChangedEvent;
import com.quartz.checkin.event.TicketCategoryChangedEvent;
import com.quartz.checkin.event.TicketStatusChangedEvent;
import com.quartz.checkin.repository.CategoryRepository;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.repository.TicketLogRepository;
import com.quartz.checkin.repository.TicketRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketLogServiceImpl implements TicketLogService {

    private final TicketRepository ticketRepository;
    private final TicketLogRepository ticketLogRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final WebhookService webhookService;

    private static final Set<Character> ENGLISH_VOWELS = Set.of('a', 'e', 'i', 'o', 'u',
            'A', 'E', 'I', 'O', 'U');

    @Override
    public TicketLogResponse closeTicket(Long memberId, Long ticketId) {

        Ticket ticket = getValidTicket(ticketId);
        Member manager = getValidMember(memberId);

        validateTicketForUpdate(ticket, manager, true, true, false);
        ticket.closeTicket();
        ticketRepository.save(ticket);

        String subjectParticle = getSubjectParticle(manager.getUsername());
        String objectParticle = getObjectParticle(ticket.getTitle());

        String logContent = String.format("%s%s %s%s 완료했습니다.",
                manager.getUsername(), subjectParticle, ticket.getTitle(), objectParticle);

        TicketLog ticketLog = TicketLog.builder()
                .ticket(ticket)
                .logType(LogType.STATUS)
                .content(logContent)
                .createdAt(LocalDateTime.now())
                .build();

        ticketLogRepository.save(ticketLog);
        eventPublisher.publishEvent(new TicketStatusChangedEvent(ticket.getId(), ticket.getCustomId(), ticket.getAgitId(), 2));

        return new TicketLogResponse(ticketLog);
    }

    @Override
    public TicketLogResponse updateFirstCategory(Long memberId, Long ticketId, FirstCategoryPatchRequest request) {

        Ticket ticket = getValidTicket(ticketId);
        Member manager = getValidMember(memberId);
        validateTicketForUpdate(ticket, manager, false, false, false);

        String oldFirstCategory = ticket.getFirstCategory().getName();
        String oldCustomId = ticket.getCustomId();

        Category newFirstCategory = categoryRepository.findByNameAndParentIsNull(request.getFirstCategory())
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST));

        List<Category> secondCategories = categoryRepository.findByParentOrderByIdAsc(newFirstCategory);
        if (secondCategories.isEmpty()) {
            throw new ApiException(ErrorCode.CATEGORY_NOT_FOUND_SECOND);
        }
        Category newSecondCategory = secondCategories.get(0);

        String newCustomId = updateTicketCategoryAndCustomId(ticket, newFirstCategory, newSecondCategory);
        return createAndSaveTicketLog(ticket, manager, oldFirstCategory, null, newFirstCategory.getName(), newSecondCategory.getName(), oldCustomId, newCustomId, "first");
    }

    @Override
    public TicketLogResponse updateSecondCategory(Long memberId, Long ticketId, Long firstCategoryId, SecondCategoryPatchRequest request) {
        Ticket ticket = getValidTicket(ticketId);
        Member manager = getValidMember(memberId);

        validateTicketForUpdate(ticket, manager, false, false, false);

        Category firstCategory = ticket.getFirstCategory();
        if (!firstCategory.getId().equals(firstCategoryId)) {
            throw new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST);
        }

        String oldSecondCategory = ticket.getSecondCategory().getName();
        String oldCustomId = ticket.getCustomId();

        Category newSecondCategory = categoryRepository.findByNameAndParent(request.getSecondCategory(), firstCategory)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_SECOND));

        if (oldSecondCategory.equals(newSecondCategory.getName())) {
            return new TicketLogResponse(null);
        }

        String newCustomId = updateTicketCategoryAndCustomId(ticket, firstCategory, newSecondCategory);

        return createAndSaveTicketLog(ticket, manager, null, oldSecondCategory, null, newSecondCategory.getName(), oldCustomId, newCustomId, "second");
    }


    @Override
    public TicketLogResponse assignManager(Long memberId, Long ticketId, String managerUsername) {

        Ticket ticket = getValidTicket(ticketId);
        Member currentManager = ticket.getManager();
        Member manager = memberRepository.findByUsername(managerUsername)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));

        if (currentManager != null && !currentManager.getId().equals(memberId) && ticket.getStatus() != Status.OPEN) {
            throw new ApiException(ErrorCode.TICKET_ALREADY_ASSIGNED);
        }

        if (currentManager == null) {
            ticket.updateStatus(Status.IN_PROGRESS);
            ticket.updatePriority(Priority.MEDIUM);
        }

        validateTicketForUpdate(ticket, manager, false, false, true);

        ticket.assignManager(manager);
        ticketRepository.save(ticket);

        String logContent;
        if (currentManager != null) {
            logContent = String.format("담당자가 변경되었습니다. %s → %s",
                    currentManager.getUsername(),
                    manager.getUsername());
        } else {
            String subjectParticle = getSubjectParticle(ticket.getTitle()); // 조사 적용
            logContent = String.format("%s%s %s에게 배정되었습니다.",
                    ticket.getTitle(), subjectParticle, manager.getUsername());
        }

        TicketLog ticketLog = TicketLog.builder()
                .ticket(ticket)
                .logType(LogType.MANAGER)
                .content(logContent)
                .createdAt(LocalDateTime.now())
                .build();

        ticketLogRepository.save(ticketLog);

        eventPublisher.publishEvent(new TicketStatusChangedEvent(ticket.getId(), ticket.getCustomId(), ticket.getAgitId(), 1));

        List<String> assigneesForInProgress = new ArrayList<>();
        if (ticket.getUser() != null) {
            assigneesForInProgress.add(ticket.getUser().getUsername());
        }
        assigneesForInProgress.add(manager.getUsername());

        eventPublisher.publishEvent(new TicketAssigneeChangedEvent(
                ticket.getAgitId(),
                manager.getId(),
                ticket.getUser().getId(),
                ticket.getId(),
                new ArrayList<>(assigneesForInProgress)
        ));

        return new TicketLogResponse(ticketLog);
    }

    private Ticket getValidTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));
    }

    private Member getValidMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private void validateTicketForUpdate(Ticket ticket, Member manager, boolean checkCompleted, boolean checkStatus, boolean checkReassign) {

        if (checkCompleted && ticket.getStatus() == Status.CLOSED) {
            throw new ApiException(ErrorCode.CANNOT_CHANGE_COMPLETED_TICKET);
        }

        if (checkStatus && ticket.getStatus() != Status.IN_PROGRESS) {
            throw new ApiException(ErrorCode.INVALID_TICKET_STATUS);
        }

        if (checkReassign && ticket.getManager() != null && ticket.getManager().getId().equals(manager.getId())) {
            throw new ApiException(ErrorCode.TICKET_ALREADY_ASSIGNED_TO_SELF);
        }

        if (!checkReassign && (ticket.getManager() == null || !ticket.getManager().getId().equals(manager.getId()))) {
            throw new ApiException(ErrorCode.INVALID_TICKET_MANAGER);
        }
    }

    private String updateTicketCategoryAndCustomId(Ticket ticket, Category firstCategory, Category secondCategory) {

        String firstCategoryAlias = firstCategory.getAlias();
        String secondCategoryAlias = secondCategory.getAlias();

        String datePart = ticket.getCreatedAt().format(DateTimeFormatter.ofPattern("MMdd"));
        String oldCustomId = ticket.getCustomId();
        String numberPart = oldCustomId.substring(oldCustomId.length() - 3);
        String newCustomId = datePart + firstCategoryAlias + "-" + secondCategoryAlias + numberPart;

        ticket.updateCategory(firstCategory, secondCategory);
        ticket.updateCustomId(newCustomId);
        ticketRepository.save(ticket);

        return newCustomId;
    }

    private TicketLogResponse createAndSaveTicketLog(
            Ticket ticket, Member manager, String oldFirstCategory, String oldSecondCategory,
            String newFirstCategory, String newSecondCategory, String oldCustomId, String newCustomId, String actionType) {

        String subjectParticle = getSubjectParticle(manager.getUsername());

        String logContent;
        if (actionType.equals("first")) {
            logContent = String.format(
                    "%s%s 1차 카테고리를 변경하였습니다. '%s' → '%s'. (2차 카테고리 기본값: %s)",
                    manager.getUsername(),
                    subjectParticle,
                    oldFirstCategory,
                    newFirstCategory,
                    newSecondCategory
            );
        } else {
            logContent = String.format(
                    "%s%s 2차 카테고리를 변경하였습니다. '%s' → '%s'",
                    manager.getUsername(),
                    subjectParticle,
                    oldSecondCategory,
                    newSecondCategory
            );
        }

        if (!oldCustomId.equals(newCustomId)) {
            logContent += String.format("\n티켓 번호가 변경되었습니다. '%s' → '%s'", oldCustomId, newCustomId);
        }

        TicketLog ticketLog = TicketLog.builder()
                .ticket(ticket)
                .logType(LogType.CATEGORY)
                .content(logContent)
                .createdAt(LocalDateTime.now())
                .build();

        ticketLogRepository.save(ticketLog);

        eventPublisher.publishEvent(new TicketCategoryChangedEvent(ticket.getId(), ticket.getCustomId(), ticket.getAgitId(), manager.getId(),
                oldFirstCategory != null ? oldFirstCategory : oldSecondCategory,
                newFirstCategory != null ? newFirstCategory : newSecondCategory,
                logContent));

        return new TicketLogResponse(ticketLog);
    }

    public static String getSubjectParticle(String word) {
        if (word == null || word.isEmpty()) return "이";

        char lastChar = word.charAt(word.length() - 1);
        boolean isEnglish = word.chars().allMatch(Character::isLetter);

        if (isEnglish) {
            return ENGLISH_VOWELS.contains(lastChar) ? "가" : "이";
        }

        return (lastChar >= '가' && lastChar <= '힣' && (lastChar - '가') % 28 != 0) ? "이" : "가";
    }

    public static String getObjectParticle(String word) {
        if (word == null || word.isEmpty()) return "을";

        char lastChar = word.charAt(word.length() - 1);
        boolean isEnglish = word.chars().allMatch(Character::isLetter);

        if (isEnglish) {
            return ENGLISH_VOWELS.contains(lastChar) ? "를" : "을";
        }
        return (lastChar >= '가' && lastChar <= '힣' && (lastChar - '가') % 28 != 0) ? "을" : "를";
    }
}
