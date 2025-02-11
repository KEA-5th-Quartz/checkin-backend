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

    // 티켓 상태 변경: 진행 중 → 완료
    @Override
    public TicketLogResponse closeTicket(Long memberId, Long ticketId) {

        // 티켓 & 담당자 조회
        Ticket ticket = getValidTicket(ticketId);
        Member manager = getValidMember(memberId);

        // 예외 검증 (완료된 티켓 변경 불가, 상태 체크, 담당자 본인 확인)
        validateTicketForUpdate(ticket, manager, true, true, false);

        // 상태 변경 적용
        ticket.closeTicket();
        ticketRepository.save(ticket);

        // 조사 처리
        String subjectParticle = getSubjectParticle(manager.getUsername());
        String objectParticle = getObjectParticle(ticket.getTitle());

        // 로그 기록 문구 생성
        String logContent = String.format("%s%s %s%s 완료했습니다.",
                manager.getUsername(), subjectParticle, ticket.getTitle(), objectParticle);

        // 로그 저장
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
        // 티켓 & 담당자 조회
        Ticket ticket = getValidTicket(ticketId);
        Member manager = getValidMember(memberId);

        // 예외 검증
        validateTicketForUpdate(ticket, manager, false, false, false);

        // 기존 카테고리 정보 저장
        String oldFirstCategory = ticket.getFirstCategory().getName();
        String oldCustomId = ticket.getCustomId();

        // 새로운 1차 카테고리 조회
        Category newFirstCategory = categoryRepository.findByNameAndParentIsNull(request.getFirstCategory())
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST));

        // 새로운 1차 카테고리에 해당하는 2차 카테고리 중 첫 번째 선택
        List<Category> secondCategories = categoryRepository.findByParentOrderByIdAsc(newFirstCategory);
        if (secondCategories.isEmpty()) {
            throw new ApiException(ErrorCode.CATEGORY_NOT_FOUND_SECOND);
        }
        Category newSecondCategory = secondCategories.get(0);

        // 카테고리 & customId 업데이트
        String newCustomId = updateTicketCategoryAndCustomId(ticket, newFirstCategory, newSecondCategory);

        // 로그 저장 및 이벤트 발행
        return createAndSaveTicketLog(ticket, manager, oldFirstCategory, null, newFirstCategory.getName(), newSecondCategory.getName(), oldCustomId, newCustomId, "first");
    }

    @Override
    public TicketLogResponse updateSecondCategory(Long memberId, Long ticketId, Long firstCategoryId, SecondCategoryPatchRequest request) {
        // 티켓 & 담당자 조회
        Ticket ticket = getValidTicket(ticketId);
        Member manager = getValidMember(memberId);

        // 예외 검증 (수정 가능 여부 확인)
        validateTicketForUpdate(ticket, manager, false, false, false);

        // 현재 1차 카테고리가 맞는지 확인
        Category firstCategory = ticket.getFirstCategory();
        if (!firstCategory.getId().equals(firstCategoryId)) {
            throw new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST);
        }

        // 기존 정보 저장
        String oldSecondCategory = ticket.getSecondCategory().getName();
        String oldCustomId = ticket.getCustomId();

        // 새로운 2차 카테고리 조회
        Category newSecondCategory = categoryRepository.findByNameAndParent(request.getSecondCategory(), firstCategory)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_SECOND));

        if (oldSecondCategory.equals(newSecondCategory.getName())) {
            return new TicketLogResponse(null);
        }

        // 공통 메서드 호출하여 카테고리 & customId 업데이트
        String newCustomId = updateTicketCategoryAndCustomId(ticket, firstCategory, newSecondCategory);

        // 공통 메서드 호출하여 로그 저장 및 이벤트 발행
        return createAndSaveTicketLog(ticket, manager, null, oldSecondCategory, null, newSecondCategory.getName(), oldCustomId, newCustomId, "second");
    }


    @Override
    public TicketLogResponse assignManager(Long memberId, Long ticketId, String managerUsername) {
        // 티켓 & 기존 담당자 조회
        Ticket ticket = getValidTicket(ticketId);
        Member currentManager = ticket.getManager();
        Member manager = memberRepository.findByUsername(managerUsername)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));

        // 예외 처리
        if (currentManager != null && !currentManager.getId().equals(memberId) && ticket.getStatus() != Status.OPEN) {
            throw new ApiException(ErrorCode.TICKET_ALREADY_ASSIGNED);
        }
        if (currentManager == null) {
            ticket.updateStatus(Status.IN_PROGRESS);
            ticket.updatePriority(Priority.MEDIUM);
        }

        // 예외 검증
        validateTicketForUpdate(ticket, manager, false, false, true);

        // 담당자 변경
        ticket.assignManager(manager);
        ticketRepository.save(ticket);

        // 로그 기록
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

    // 특정 티켓 조회
    private Ticket getValidTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));
    }

    // 특정 담당자 조회
    private Member getValidMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));
    }

    // 티켓 업데이트를 위한 검증 메서드
    private void validateTicketForUpdate(Ticket ticket, Member manager, boolean checkCompleted, boolean checkStatus, boolean checkReassign) {

        // 이미 완료된 티켓이면 변경 불가
        if (checkCompleted && ticket.getStatus() == Status.CLOSED) {
            throw new ApiException(ErrorCode.CANNOT_CHANGE_COMPLETED_TICKET);
        }

        // 진행 중(IN_PROGRESS) 상태에서만 완료 가능
        if (checkStatus && ticket.getStatus() != Status.IN_PROGRESS) {
            throw new ApiException(ErrorCode.INVALID_TICKET_STATUS);
        }

        // 본인이 본인에게 재할당하는 경우 방지
        if (checkReassign && ticket.getManager() != null && ticket.getManager().getId().equals(manager.getId())) {
            throw new ApiException(ErrorCode.TICKET_ALREADY_ASSIGNED_TO_SELF);
        }

        // 담당자가 본인이 맞는지 검증
        if (!checkReassign && (ticket.getManager() == null || !ticket.getManager().getId().equals(manager.getId()))) {
            throw new ApiException(ErrorCode.INVALID_TICKET_MANAGER);
        }
    }

    private String updateTicketCategoryAndCustomId(Ticket ticket, Category firstCategory, Category secondCategory) {
        // customId 변경 로직
        String firstCategoryAlias = firstCategory.getAlias();
        String secondCategoryAlias = secondCategory.getAlias();

        // 기존 날짜 유지
        String datePart = ticket.getCreatedAt().format(DateTimeFormatter.ofPattern("MMdd"));
        String oldCustomId = ticket.getCustomId();
        String numberPart = oldCustomId.substring(oldCustomId.length() - 3);

        // 새 customId 생성
        String newCustomId = datePart + firstCategoryAlias + "-" + secondCategoryAlias + numberPart;

        // 카테고리 및 customId 업데이트
        ticket.updateCategory(firstCategory, secondCategory);
        ticket.updateCustomId(newCustomId);
        ticketRepository.save(ticket);

        return newCustomId;
    }

    private TicketLogResponse createAndSaveTicketLog(
            Ticket ticket, Member manager, String oldFirstCategory, String oldSecondCategory,
            String newFirstCategory, String newSecondCategory, String oldCustomId, String newCustomId, String actionType) {

        // 이/가 조사 적용
        String subjectParticle = getSubjectParticle(manager.getUsername());

        // 기본 로그 메시지
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

        // customId 변경 로그 추가
        if (!oldCustomId.equals(newCustomId)) {
            logContent += String.format("\n티켓 번호가 변경되었습니다. '%s' → '%s'", oldCustomId, newCustomId);
        }

        // 로그 저장
        TicketLog ticketLog = TicketLog.builder()
                .ticket(ticket)
                .logType(LogType.CATEGORY)
                .content(logContent)
                .createdAt(LocalDateTime.now())
                .build();

        ticketLogRepository.save(ticketLog);

        // 이벤트 발행
        eventPublisher.publishEvent(new TicketCategoryChangedEvent(ticket.getId(), ticket.getCustomId(), ticket.getAgitId(), manager.getId(),
                oldFirstCategory != null ? oldFirstCategory : oldSecondCategory,
                newFirstCategory != null ? newFirstCategory : newSecondCategory,
                logContent));

        return new TicketLogResponse(ticketLog);
    }

    // 주격 조사 검증 로직
    public static String getSubjectParticle(String word) {
        if (word == null || word.isEmpty()) return "이";

        char lastChar = word.charAt(word.length() - 1);
        boolean isEnglish = word.chars().allMatch(Character::isLetter); // 전체가 영어인지 확인

        if (isEnglish) { // 영어 단어의 마지막 글자가 모음이면 "가", 자음이면 "이"
            return ENGLISH_VOWELS.contains(lastChar) ? "가" : "이";
        }

        // 한글 받침 여부에 따라 "이" 또는 "가"
        return (lastChar >= '가' && lastChar <= '힣' && (lastChar - '가') % 28 != 0) ? "이" : "가";
    }

    // 목적격 조사 검증 로직
    public static String getObjectParticle(String word) {
        if (word == null || word.isEmpty()) return "을";

        char lastChar = word.charAt(word.length() - 1);
        boolean isEnglish = word.chars().allMatch(Character::isLetter); // 전체가 영어인지 확인

        if (isEnglish) { // 영어 단어의 마지막 글자가 모음이면 "를", 자음이면 "을"
            return ENGLISH_VOWELS.contains(lastChar) ? "를" : "을";
        }

        // 한글 받침 여부에 따라 "을" 또는 "를"
        return (lastChar >= '가' && lastChar <= '힣' && (lastChar - '가') % 28 != 0) ? "을" : "를";
    }
}
