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
import com.quartz.checkin.entity.QMember;
import com.quartz.checkin.entity.QTicket;
import com.quartz.checkin.entity.Role;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.entity.TicketAttachment;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.repository.TicketAttachmentRepository;
import com.quartz.checkin.repository.TicketRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketQueryServiceImpl implements TicketQueryService {

    private final TicketRepository ticketRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final MemberRepository memberRepository;
    private final JPAQueryFactory queryFactory;


    @Override
    public TicketDetailResponse getTicketDetail(Long memberId, Long ticketId) {
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

    @Override
    public ManagerTicketListResponse getManagerTickets(Long memberId, List<Status> statuses, List<String> usernames,
                                                       List<String> categories, List<Priority> priorities,
                                                       Boolean dueToday, Boolean dueThisWeek, int page, int size, String sortByCreatedAt) {

        Page<Ticket> ticketPage = fetchTickets(memberId, null, statuses, usernames, categories, priorities, dueToday, dueThisWeek, page, size, sortByCreatedAt);
        return TicketResponseConverter.toManagerTicketListResponse(ticketPage);
    }

    @Override
    public UserTicketListResponse getUserTickets(Long userId, List<Status> statuses, List<String> usernames,
                                                 List<String> categories, List<Priority> priorities,
                                                 Boolean dueToday, Boolean dueThisWeek, int page, int size, String sortByCreatedAt) {
        Page<Ticket> ticketPage = fetchTickets(userId, null, statuses, usernames, categories, priorities, dueToday, dueThisWeek, page, size, sortByCreatedAt);
        return TicketResponseConverter.toUserTicketListResponse(ticketPage);
    }

    @Override
    public ManagerTicketListResponse searchManagerTickets(Long memberId, String keyword, int page, int size, String sortByCreatedAt) {
        Page<Ticket> ticketPage = fetchTickets(null, keyword, null, null, null, null, null, null, page, size, sortByCreatedAt);
        return TicketResponseConverter.toManagerTicketListResponse(ticketPage);
    }

    @Override
    public UserTicketListResponse searchUserTickets(Long memberId, String keyword, int page, int size, String sortByCreatedAt) {
        Page<Ticket> ticketPage = fetchTickets(memberId, keyword, null, null, null, null, null, null, page, size, sortByCreatedAt);
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

    private Page<Ticket> fetchTickets(
            Long memberId, String keyword, List<Status> statuses, List<String> usernames,
            List<String> categories, List<Priority> priorities,
            Boolean dueToday, Boolean dueThisWeek, int page, int size, String sortByCreatedAt) {

        validatePagination(page, size);

        Sort.Direction createdAtSortDirection = "asc".equalsIgnoreCase(sortByCreatedAt) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(createdAtSortDirection, "createdAt");
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        QTicket ticket = QTicket.ticket;
        QMember manager = QMember.member;

        LocalDate today = LocalDate.now();
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        BooleanBuilder whereClause = new BooleanBuilder();
        whereClause.and(ticket.deletedAt.isNull());

        if (memberId != null) {
            whereClause.and(ticket.user.id.eq(memberId));
        }

        if (keyword != null && !keyword.isBlank()) {
            String searchKeyword = "%" + keyword.toLowerCase() + "%";
            whereClause.and(ticket.title.lower().like(searchKeyword)
                    .or(ticket.content.lower().like(searchKeyword)));
        }

        if (statuses != null && !statuses.isEmpty()) {
            whereClause.and(ticket.status.in(statuses));
        }

        if (categories != null && !categories.isEmpty()) {
            whereClause.and(ticket.firstCategory.name.lower().trim().in(categories));
        }

        if (priorities != null && !priorities.isEmpty()) {
            whereClause.and(ticket.priority.in(priorities));
        }

        if (usernames != null && !usernames.isEmpty()) {
            whereClause.and(ticket.manager.username.in(usernames));
        }

        if (Boolean.TRUE.equals(dueToday)) {
            whereClause.and(ticket.dueDate.eq(today));
        }

        if (Boolean.TRUE.equals(dueThisWeek)) {
            whereClause.and(ticket.dueDate.between(today, endOfWeek));
        }

        OrderSpecifier<?> orderSpecifier = "asc".equalsIgnoreCase(sortByCreatedAt)
                ? ticket.createdAt.asc()
                : ticket.createdAt.desc();

        // QueryDSL로 페이징 적용한 조회 쿼리 실행
        List<Ticket> results = queryFactory
                .selectFrom(ticket)
                .leftJoin(ticket.manager, manager).fetchJoin()
                .where(whereClause)
                .orderBy(orderSpecifier)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 총 개수 조회
        long safeTotalCount = Optional.ofNullable(
                queryFactory.select(ticket.count())
                        .from(ticket)
                        .where(whereClause)
                        .fetchOne()
        ).orElse(0L);

        return new PageImpl<>(results, pageable, safeTotalCount);
    }

    private void validatePagination(int page, int size) {
        if (page < 1) throw new ApiException(ErrorCode.INVALID_PAGE_NUMBER);
        if (size <= 0) throw new ApiException(ErrorCode.INVALID_PAGE_SIZE);
    }
}