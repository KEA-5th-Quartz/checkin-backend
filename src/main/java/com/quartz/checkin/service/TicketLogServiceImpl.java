package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.request.CategoryUpdateRequest;
import com.quartz.checkin.dto.request.PriorityUpdateRequest;
import com.quartz.checkin.dto.response.TicketLogResponse;
import com.quartz.checkin.entity.*;
import com.quartz.checkin.repository.CategoryRepository;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.repository.TicketLogRepository;
import com.quartz.checkin.repository.TicketRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TicketLogServiceImpl implements TicketLogService {

    private final TicketRepository ticketRepository;
    private final TicketLogRepository ticketLogRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;

    private static final Set<Character> ENGLISH_VOWELS = Set.of('a', 'e', 'i', 'o', 'u',
            'A', 'E', 'I', 'O', 'U');
    // Todo: 담당자 배정 시, 자동으로 중요도 보통으로 설정
    // 담당자 배정
    @Transactional
    @Override
    public TicketLogResponse assignManager(Long memberId, Long ticketId) {

        // 티켓 & 담당자 조회
        Ticket ticket = getValidTicket(ticketId);
        Member manager = getValidMember(memberId);

        // 예외 검증
        validateTicketForUpdate(ticket, manager, false, false, true);

        // 조사 처리
        String subjectParticle = getSubjectParticle(ticket.getTitle());

        // 담당자 배정 처리
        ticket.assignManager(manager);
        ticketRepository.save(ticket);

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
        return new TicketLogResponse(ticketLog);
    }

    // 카테고리 변경
    @Transactional
    @Override
    public TicketLogResponse updateCategory(Long memberId, Long ticketId, CategoryUpdateRequest request) {

        // 티켓 & 담당자 조회
        Ticket ticket = getValidTicket(ticketId);
        Member manager = getValidMember(memberId);

        // 예외 검증
        validateTicketForUpdate(ticket, manager, false, false, false);

        // 새로운 카테고리 조회
        Category newFirstCategory = categoryRepository.findByNameAndParentIsNull(request.getFirstCategory())
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST));

        Category newSecondCategory = categoryRepository.findByNameAndParent(request.getSecondCategory(), newFirstCategory)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_SECOND));

        // 기존 카테고리 저장
        String oldFirstCategory = ticket.getFirstCategory().getName();
        String oldSecondCategory = ticket.getSecondCategory().getName();
        boolean isFirstCategoryChanged = !oldFirstCategory.equals(newFirstCategory.getName());
        boolean isSecondCategoryChanged = !oldSecondCategory.equals(newSecondCategory.getName());

        // 카테고리 업데이트
        ticket.updateCategory(newFirstCategory, newSecondCategory);
        ticketRepository.save(ticket); // 변경 감지

        // 이/가 조사 적용
        String subjectParticle = getSubjectParticle(manager.getUsername());
        String logContent;

        if (isFirstCategoryChanged && isSecondCategoryChanged) {
            logContent = String.format(
                    "%s%s 1차 및 2차 카테고리를 변경하였습니다. %s → %s, %s → %s",
                    manager.getUsername(),
                    subjectParticle,
                    oldFirstCategory, newFirstCategory.getName(),
                    oldSecondCategory, newSecondCategory.getName()
            );
        } else if (isFirstCategoryChanged) {
            logContent = String.format(
                    "%s%s 1차 카테고리를 변경하였습니다. %s → %s",
                    manager.getUsername(),
                    subjectParticle,
                    oldFirstCategory, newFirstCategory.getName()
            );
        } else if (isSecondCategoryChanged) {
            logContent = String.format(
                    "%s%s 2차 카테고리를 변경하였습니다. %s → %s",
                    manager.getUsername(),
                    subjectParticle,
                    oldSecondCategory, newSecondCategory.getName()
            );
        } else {
            throw new ApiException(ErrorCode.INVALID_TICKET_CATEGORY);
        }

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
    public TicketLogResponse reassignManager(Long memberId, Long ticketId, String newManagerUsername) {
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
