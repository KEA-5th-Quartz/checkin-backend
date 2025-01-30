package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.converter.TicketResponseConverter;
import com.quartz.checkin.dto.request.TicketCreateRequest;
import com.quartz.checkin.dto.response.*;
import com.quartz.checkin.entity.*;
import com.quartz.checkin.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketCrudServiceImpl implements TicketCrudService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;

    private void validatePagination(int page, int size) {
        if (page < 1) throw new ApiException(ErrorCode.INVALID_PAGE_NUMBER);
        if (size <= 0) throw new ApiException(ErrorCode.INVALID_PAGE_SIZE);
    }

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
    public TicketDetailResponse getTicketDetail(Long memberId, Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));

        return TicketDetailResponse.from(ticket);
    }

    @Transactional(readOnly = true)
    @Override
    public ManagerTicketListResponse getManagerTickets(
            Long memberId, List<Status> statuses, List<String> usernames,
            List<String> categories, List<Priority> priorities,
            Boolean dueToday, Boolean dueThisWeek, int page, int size) {

        Page<Ticket> ticketPage = getTickets(
                null, statuses, usernames, categories, priorities, dueToday, dueThisWeek, page, size);

        return TicketResponseConverter.toManagerTicketListResponse(ticketPage);
    }

    @Transactional(readOnly = true)
    @Override
    public UserTicketListResponse getUserTickets(Long memberId, List<Status> statuses, List<String> usernames,
                                                 List<String> categories, List<Priority> priorities,
                                                 Boolean dueToday, Boolean dueThisWeek, int page, int size) {

        Page<Ticket> ticketPage = getTickets(
                memberId, statuses, usernames, categories, priorities, dueToday, dueThisWeek, page, size);

        return TicketResponseConverter.toUserTicketListResponse(ticketPage);
    }

    private Page<Ticket> getTickets(Long memberId, List<Status> statuses, List<String> usernames,
                                    List<String> categories, List<Priority> priorities,
                                    Boolean dueToday, Boolean dueThisWeek, int page, int size) {

        validatePagination(page, size);

        Pageable pageable = PageRequest.of(page - 1, size,
                Sort.by(Sort.Direction.ASC, "dueDate")  // 마감기한 임박한 순
                        .and(Sort.by(Sort.Direction.ASC, "title"))); // 같은 마감기한일 경우 제목 가나다 순 정렬

        boolean isDueToday = Boolean.TRUE.equals(dueToday);
        boolean isDueThisWeek = Boolean.TRUE.equals(dueThisWeek);

        // 오늘 날짜와 이번 주 마지막 날 계산
        LocalDate today = LocalDate.now();
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        if (memberId == null) {
            // 관리자용 전체 티켓 조회
            return ticketRepository.findManagerTickets(statuses, usernames, categories, priorities,
                    isDueToday, isDueThisWeek, endOfWeek, pageable);
        } else {
            // 특정 사용자의 티켓 조회
            return ticketRepository.findUserTickets(memberId, statuses, usernames, categories, priorities,
                    isDueToday, isDueThisWeek, endOfWeek, pageable);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public ManagerTicketListResponse searchManagerTickets(Long memberId, String keyword, int page, int size) {

        validatePagination(page, size);

        Pageable pageable = PageRequest.of(page - 1, size,
                Sort.by(Sort.Direction.ASC, "dueDate")  // 마감기한 임박한 순
                        .and(Sort.by(Sort.Direction.ASC, "title"))); // 같은 마감기한일 경우 제목 가나다 순 정렬
        String searchKeyword = (keyword == null || keyword.isBlank()) ? "" : keyword.toLowerCase();

        Page<Ticket> ticketPage = ticketRepository.searchTickets(searchKeyword, pageable);
        return TicketResponseConverter.toManagerTicketListResponse(ticketPage);
    }

    @Transactional(readOnly = true)
    @Override
    public UserTicketListResponse searchUserTickets(Long memberId, String keyword, int page, int size) {

        validatePagination(page, size);

        Pageable pageable = PageRequest.of(page - 1, size,
                Sort.by(Sort.Direction.ASC, "dueDate")  // 마감기한 임박한 순
                        .and(Sort.by(Sort.Direction.ASC, "title"))); // 같은 마감기한일 경우 제목 가나다 순 정렬

        Page<Ticket> ticketPage = ticketRepository.searchMyTickets(
                memberId, (keyword != null && !keyword.isBlank()) ? keyword : null, pageable);

        return TicketResponseConverter.toUserTicketListResponse(ticketPage);
    }
}
