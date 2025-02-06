package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.category.request.FirstCategoryUpdateRequest;
import com.quartz.checkin.dto.category.request.SecondCategoryUpdateRequest;
import com.quartz.checkin.dto.ticket.response.TicketLogResponse;
import com.quartz.checkin.entity.Category;
import com.quartz.checkin.entity.LogType;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.entity.TicketLog;
import com.quartz.checkin.repository.CategoryRepository;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.repository.TicketLogRepository;
import com.quartz.checkin.repository.TicketRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketLogServiceImpl implements TicketLogService {

    private final TicketRepository ticketRepository;
    private final TicketLogRepository ticketLogRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final WebhookService webhookService;

    private static final Set<Character> ENGLISH_VOWELS = Set.of('a', 'e', 'i', 'o', 'u',
            'A', 'E', 'I', 'O', 'U');

    // 담당자 배정
    @Transactional
    @Override
    public TicketLogResponse assignManager(Long memberId, String ticketId) {

        // 티켓 & 담당자 조회
        Member manager = getValidMember(memberId);
        Ticket ticket = getValidTicket(ticketId);

        // 예외 검증
        validateTicketForUpdate(ticket, manager, false, false, true);

        // 조사 처리
        String subjectParticle = getSubjectParticle(ticket.getTitle());

        // 담당자 배정 처리
        ticket.assignManager(manager);
        ticketRepository.save(ticket);

        // 상태 변경 요청
        webhookService.updateStatusInWebhook(ticket.getAgitId(), 1);

        // 웹훅 담당자 변경 요청
        webhookService.updateAssigneeInWebhook(ticket.getAgitId(), manager.getId(), ticket.getUser().getId());

        // 로그 기록
        String logContent = String.format("%s%s %s에게 배정되었습니다.",
                ticket.getTitle(), subjectParticle, manager.getUsername());

        TicketLog ticketLog = TicketLog.builder()
                .ticket(ticket)
                .logType(LogType.MANAGER)
                .content(logContent)
                .build();

        ticketLogRepository.save(ticketLog);
        return new TicketLogResponse(ticketLog);
    }

    // 티켓 상태 변경: 진행 중 → 완료
    @Transactional
    @Override
    public TicketLogResponse closeTicket(Long memberId, String ticketId) {

        // 티켓 & 담당자 조회
        Ticket ticket = getValidTicket(ticketId);
        Member manager = getValidMember(memberId);

        // 예외 검증 (완료된 티켓 변경 불가, 상태 체크, 담당자 본인 확인)
        validateTicketForUpdate(ticket, manager, true, true, false);

        // 상태 변경 적용
        ticket.closeTicket();
        ticketRepository.save(ticket);

        // 웹훅 상태 변경 요청
        webhookService.updateStatusInWebhook(ticket.getAgitId(), 2);

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
        return new TicketLogResponse(ticketLog);
    }

    // 1차 카테고리 변경
    @Override
    public TicketLogResponse updateFirstCategory(Long memberId, String ticketId, FirstCategoryUpdateRequest request) {

        // 티켓 & 담당자 조회
        Ticket ticket = getValidTicket(ticketId);
        Member manager = getValidMember(memberId);

        // 예외 검증
        validateTicketForUpdate(ticket, manager, false, false, false);

        // 기존 카테고리 정보 저장
        String oldFirstCategory = ticket.getFirstCategory().getName();
        Category newFirstCategory = categoryRepository.findByNameAndParentIsNull(request.getName())
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST));

        List<Category> secondCategories = categoryRepository.findByParentOrderByIdAsc(newFirstCategory);
        if (secondCategories.isEmpty()) {
            throw new ApiException(ErrorCode.CATEGORY_NOT_FOUND_SECOND);
        }
        Category newSecondCategory = secondCategories.get(0); // 가장 첫 번째 2차 카테고리 선택

        // 카테고리 업데이트
        ticket.updateCategory(newFirstCategory, newSecondCategory);
        ticketRepository.save(ticket);

        // 이/가 조사 적용
        String subjectParticle = getSubjectParticle(manager.getUsername());
        String logContent = String.format(
                "%s%s 1차 카테고리를 변경하였습니다. '%s' → '%s'. (2차 카테고리 기본값: %s)",
                manager.getUsername(),
                subjectParticle,
                oldFirstCategory,
                newFirstCategory.getName(),
                newSecondCategory.getName()
        );

        // 웹훅 댓글로 카테고리 변경 알림
        webhookService.addCommentToWebhookPost(ticket.getAgitId(), logContent);

        // 로그 저장
        TicketLog ticketLog = TicketLog.builder()
                .ticket(ticket)
                .logType(LogType.CATEGORY)
                .content(logContent)
                .createdAt(LocalDateTime.now())
                .build();

        ticketLogRepository.save(ticketLog);
        return new TicketLogResponse(ticketLog);
    }

    @Transactional
    @Override
    public TicketLogResponse updateSecondCategory(Long memberId, String ticketId, Long firstCategoryId, SecondCategoryUpdateRequest request) {

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

        // 기존 2차 카테고리 저장
        String oldSecondCategory = ticket.getSecondCategory().getName();

        // 새로운 2차 카테고리 조회 (해당 1차 카테고리에 속하는지 확인)
        Category newSecondCategory = categoryRepository.findByNameAndParent(request.getSecondCategory(), firstCategory)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_SECOND));

        // 카테고리 업데이트
        ticket.updateCategory(firstCategory, newSecondCategory);
        ticketRepository.save(ticket); // 변경 감지

        // 이/가 조사 적용
        String subjectParticle = getSubjectParticle(manager.getUsername());

        // 로그 메시지 생성
        String logContent = String.format(
                "%s%s 2차 카테고리를 변경하였습니다. '%s' → '%s'",
                manager.getUsername(),
                subjectParticle,
                oldSecondCategory,
                newSecondCategory.getName()
        );

        // 웹훅 댓글로 카테고리 변경 알림
        webhookService.addCommentToWebhookPost(ticket.getAgitId(), logContent);

        // 로그 저장
        TicketLog ticketLog = TicketLog.builder()
                .ticket(ticket)
                .logType(LogType.CATEGORY)
                .content(logContent)
                .createdAt(LocalDateTime.now())
                .build();

        ticketLogRepository.save(ticketLog);
        return new TicketLogResponse(ticketLog);
    }

    @Transactional
    @Override
    public TicketLogResponse reassignManager(Long memberId, String ticketId, String newManagerUsername) {
        // 티켓 & 기존 담당자 조회
        Ticket ticket = getValidTicket(ticketId);
        Member currentManager = ticket.getManager();
        Member newManager = memberRepository.findByUsername(newManagerUsername)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));

        // 예외 검증
        validateTicketForUpdate(ticket, newManager, false, false, true);

        // 담당자 변경
        ticket.reassignManager(newManager);
        ticketRepository.save(ticket);

        // 웹훅 담당자 업데이트 요청
        webhookService.updateAssigneeInWebhook(ticket.getAgitId(), newManager.getId(), ticket.getUser().getId());

        // 로그 기록
        String logContent = String.format("담당자가 변경되었습니다. %s → %s",
                currentManager != null ? currentManager.getUsername() : "없음",
                newManager.getUsername());

        TicketLog ticketLog = TicketLog.builder()
                .ticket(ticket)
                .logType(LogType.MANAGER)
                .content(logContent)
                .createdAt(LocalDateTime.now())
                .build();

        ticketLogRepository.save(ticketLog);
        return new TicketLogResponse(ticketLog);
    }

    // 특정 티켓 조회
    private Ticket getValidTicket(String ticketId) {
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
