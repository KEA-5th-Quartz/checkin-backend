package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.common.exception.ApiException;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketCrudServiceImpl implements TicketCrudService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;

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

    @Transactional
    @Override
    public ManagerTicketListResponse getManagerTickets(
            Long memberId, Status status, String username, String category, Priority priority,
            Boolean dueToday, Boolean dueThisWeek, int page, int size) {

        Page<Ticket> ticketPage = getTickets(
                null, status, username, category, priority, dueToday, dueThisWeek, page, size);

        List<ManagerTicketSummaryResponse> ticketList = ticketPage.getContent().stream()
                .map(ManagerTicketSummaryResponse::from)
                .collect(Collectors.toList());

        return new ManagerTicketListResponse(
                ticketPage.getNumber() + 1,
                ticketPage.getSize(),
                ticketPage.getTotalPages(),
                (int) ticketPage.getTotalElements(),
                ticketList
        );
    }

    @Transactional
    @Override
    public UserTicketListResponse getUserTickets(Long memberId, Status status, String username, String category,
                                                 Boolean dueToday, Boolean dueThisWeek, int page, int size) {

        Page<Ticket> ticketPage = getTickets(
                memberId, status, username, category, null, dueToday, dueThisWeek, page, size);

        List<UserTicketSummaryResponse> ticketList = ticketPage.getContent().stream()
                .map(UserTicketSummaryResponse::from)
                .collect(Collectors.toList());

        return new UserTicketListResponse(
                ticketPage.getNumber() + 1,
                ticketPage.getSize(),
                ticketPage.getTotalPages(),
                (int) ticketPage.getTotalElements(),
                ticketList
        );
    }

    private Page<Ticket> getTickets(Long memberId, Status status, String username, String category, Priority priority,
                                    Boolean dueToday, Boolean dueThisWeek, int page, int size) {

        if (page < 1) throw new ApiException(ErrorCode.INVALID_PAGE_NUMBER);
        if (size <= 0) throw new ApiException(ErrorCode.INVALID_PAGE_SIZE);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        boolean isDueToday = Boolean.TRUE.equals(dueToday);
        boolean isDueThisWeek = Boolean.TRUE.equals(dueThisWeek);

        // 오늘 날짜와 이번 주 마지막 날 계산
        LocalDate today = LocalDate.now();
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        if (memberId == null) {
            // 관리자용 전체 티켓 조회
            return ticketRepository.findManagerTickets(status, username, category, priority,
                    isDueToday, isDueThisWeek, endOfWeek, pageable);
        } else {
            // 특정 사용자의 티켓 조회
            return ticketRepository.findUserTickets(memberId, status, username, category,
                    isDueToday, isDueThisWeek, endOfWeek, pageable);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public ManagerTicketListResponse searchManagerTickets(Long memberId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String searchKeyword = (keyword == null || keyword.isBlank()) ? "" : keyword.toLowerCase();

        Page<Ticket> ticketPage = ticketRepository.searchTickets(searchKeyword, pageable);

        List<ManagerTicketSummaryResponse> ticketList = ticketPage.getContent().stream()
                .map(ManagerTicketSummaryResponse::from)
                .collect(Collectors.toList());

        return new ManagerTicketListResponse(
                ticketPage.getNumber(),
                ticketPage.getSize(),
                ticketPage.getTotalPages(),
                (int) ticketPage.getTotalElements(),
                ticketList
        );
    }

    @Transactional(readOnly = true)
    @Override
    public UserTicketListResponse searchUserTickets(Long memberId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Ticket> ticketPage = ticketRepository.searchMyTickets(
                memberId, (keyword != null && !keyword.isBlank()) ? keyword : null, pageable);

        List<UserTicketSummaryResponse> ticketList = ticketPage.getContent().stream()
                .map(UserTicketSummaryResponse::from)
                .collect(Collectors.toList());

        return new UserTicketListResponse(
                ticketPage.getNumber(),
                ticketPage.getSize(),
                ticketPage.getTotalPages(),
                (int) ticketPage.getTotalElements(),
                ticketList
        );
    }
}
