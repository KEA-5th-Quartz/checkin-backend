package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.converter.TicketResponseConverter;
import com.quartz.checkin.dto.ticket.response.ManagerTicketListResponse;
import com.quartz.checkin.dto.ticket.response.TicketDetailResponse;
import com.quartz.checkin.dto.ticket.response.TicketProgressResponse;
import com.quartz.checkin.dto.ticket.response.UserTicketListResponse;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Priority;
import com.quartz.checkin.entity.Role;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.entity.TicketAttachment;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.repository.TicketAttachmentRepository;
import com.quartz.checkin.repository.TicketRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketQueryServiceImpl implements TicketQueryService {

    private final TicketRepository ticketRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    @Override
    public TicketDetailResponse getTicketDetail(Long memberId, String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));

        boolean isManager = member.getRole() == Role.MANAGER;

        if (!isManager && !ticket.getUser().getId().equals(memberId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        List<TicketAttachment> attachments = ticketAttachmentRepository.findByTicketId(ticketId);

        return TicketDetailResponse.from(ticket, attachments);
    }
    @Transactional(readOnly = true)
    @Override
    public ManagerTicketListResponse getManagerTickets(Long memberId, List<Status> statuses, List<String> usernames,
                                                       List<String> categories, List<Priority> priorities,
                                                       Boolean dueToday, Boolean dueThisWeek, int page, int size) {
        Page<Ticket> ticketPage = getTickets(null, statuses, usernames, categories, priorities, dueToday, dueThisWeek, page, size);
        return TicketResponseConverter.toManagerTicketListResponse(ticketPage);
    }

    @Transactional(readOnly = true)
    @Override
    public UserTicketListResponse getUserTickets(Long memberId, List<Status> statuses, List<String> usernames,
                                                 List<String> categories, List<Priority> priorities,
                                                 Boolean dueToday, Boolean dueThisWeek, int page, int size) {
        Page<Ticket> ticketPage = getTickets(memberId, statuses, usernames, categories, priorities, dueToday, dueThisWeek, page, size);
        return TicketResponseConverter.toUserTicketListResponse(ticketPage);
    }

    @Transactional(readOnly = true)
    @Override
    public ManagerTicketListResponse searchManagerTickets(Long memberId, String keyword, int page, int size) {
        validatePagination(page, size);

        Pageable pageable = PageRequest.of(page - 1, size,
                Sort.by(Sort.Direction.ASC, "dueDate").and(Sort.by(Sort.Direction.ASC, "title")));

        Page<Ticket> ticketPage = ticketRepository.searchTickets(keyword == null || keyword.isBlank() ? "" : keyword.toLowerCase(), pageable);
        return TicketResponseConverter.toManagerTicketListResponse(ticketPage);
    }

    @Transactional(readOnly = true)
    @Override
    public UserTicketListResponse searchUserTickets(Long memberId, String keyword, int page, int size) {
        validatePagination(page, size);

        Pageable pageable = PageRequest.of(page - 1, size,
                Sort.by(Sort.Direction.ASC, "dueDate").and(Sort.by(Sort.Direction.ASC, "title")));

        Page<Ticket> ticketPage = ticketRepository.searchMyTickets(memberId, (keyword != null && !keyword.isBlank()) ? keyword : null, pageable);
        return TicketResponseConverter.toUserTicketListResponse(ticketPage);
    }

    @Override
    public TicketProgressResponse getManagerProgress(Long memberId) {
        List<Object[]> resultList = ticketRepository.getManagerTicketStatistics(memberId);

        // 만약 결과가 비어있다면 기본값 반환
        if (resultList.isEmpty()) {
            return new TicketProgressResponse(0, 0, 0, 0, "0 / 0");
        }

        // Object 배열에서 각 값 추출
        Object[] result = resultList.get(0);

        int dueTodayCount = ((Number) result[0]).intValue();
        int openTicketCount = ((Number) result[1]).intValue();
        int inProgressTicketCount = ((Number) result[2]).intValue();
        int closedTicketCount = ((Number) result[3]).intValue();
        int totalTickets = ((Number) result[4]).intValue();

        String progressExpression = totalTickets > 0
                ? String.format("%d / %d", inProgressTicketCount + closedTicketCount, totalTickets)
                : "0 / 0";

        return new TicketProgressResponse(
                dueTodayCount,
                openTicketCount,
                inProgressTicketCount,
                closedTicketCount,
                progressExpression
        );
    }



    private Page<Ticket> getTickets(Long memberId, List<Status> statuses, List<String> usernames,
                                    List<String> categories, List<Priority> priorities,
                                    Boolean dueToday, Boolean dueThisWeek, int page, int size) {
        validatePagination(page, size);

        Pageable pageable = PageRequest.of(page - 1, size,
                Sort.by(Sort.Direction.ASC, "dueDate").and(Sort.by(Sort.Direction.ASC, "title")));

        boolean isDueToday = Boolean.TRUE.equals(dueToday);
        boolean isDueThisWeek = Boolean.TRUE.equals(dueThisWeek);

        LocalDate today = LocalDate.now();
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        return memberId == null
                ? ticketRepository.findManagerTickets(statuses, usernames, categories, priorities, isDueToday, isDueThisWeek, endOfWeek, pageable)
                : ticketRepository.findUserTickets(memberId, statuses, usernames, categories, priorities, isDueToday, isDueThisWeek, endOfWeek, pageable);
    }

    private void validatePagination(int page, int size) {
        if (page < 1) throw new ApiException(ErrorCode.INVALID_PAGE_NUMBER);
        if (size <= 0) throw new ApiException(ErrorCode.INVALID_PAGE_SIZE);
    }
}