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

        Page<Ticket> ticketPage = fetchTickets(null, statuses, usernames, categories, priorities, dueToday, dueThisWeek, page, size, sortByCreatedAt);
        validatePagination(page, size, ticketPage.getTotalPages());
        return TicketResponseConverter.toManagerTicketListResponse(ticketPage);
    }

    @Override
    public UserTicketListResponse getUserTickets(Long userId, List<Status> statuses, List<String> usernames,
                                                 List<String> categories, List<Priority> priorities,
                                                 Boolean dueToday, Boolean dueThisWeek, int page, int size, String sortByCreatedAt) {
        Page<Ticket> ticketPage = fetchTickets(userId, statuses, usernames, categories, priorities, dueToday, dueThisWeek, page, size, sortByCreatedAt);
        validatePagination(page, size, ticketPage.getTotalPages());
        return TicketResponseConverter.toUserTicketListResponse(ticketPage);
    }

    @Override
    public ManagerTicketListResponse searchManagerTickets(Long memberId, String keyword, int page, int size, String sortByCreatedAt) {
        Page<Ticket> ticketPage = fetchSearchedTickets(null, keyword, page, size, sortByCreatedAt);
        validatePagination(page, size, ticketPage.getTotalPages());
        return TicketResponseConverter.toManagerTicketListResponse(ticketPage);
    }

    @Override
    public UserTicketListResponse searchUserTickets(Long memberId, String keyword, int page, int size, String sortByCreatedAt) {
        Page<Ticket> ticketPage = fetchSearchedTickets(memberId, keyword, page, size, sortByCreatedAt);
        validatePagination(page, size, ticketPage.getTotalPages());
        return TicketResponseConverter.toUserTicketListResponse(ticketPage);
    }

    @Transactional(readOnly = true)
    @Override
    public TicketProgressResponse getManagerProgress(Long managerId) {
        QTicket ticket = QTicket.ticket;

        LocalDate today = LocalDate.now();

        Long dueTodayCount = queryFactory
                .select(ticket.count())
                .from(ticket)
                .where(ticket.deletedAt.isNull()
                        .and(ticket.dueDate.eq(today))
                        .and(ticket.manager.id.eq(managerId)))
                .fetchOne();

        Long openTicketCount = queryFactory
                .select(ticket.count())
                .from(ticket)
                .where(ticket.deletedAt.isNull()
                        .and(ticket.status.eq(Status.OPEN)))
                .fetchOne();

        Long inProgressTicketCount = queryFactory
                .select(ticket.count())
                .from(ticket)
                .where(ticket.deletedAt.isNull()
                        .and(ticket.status.eq(Status.IN_PROGRESS))
                        .and(ticket.manager.id.eq(managerId)))
                .fetchOne();

        Long closedTicketCount = queryFactory
                .select(ticket.count())
                .from(ticket)
                .where(ticket.deletedAt.isNull()
                        .and(ticket.status.eq(Status.CLOSED))
                        .and(ticket.manager.id.eq(managerId)))
                .fetchOne();

        Long totalTickets = queryFactory
                .select(ticket.count())
                .from(ticket)
                .where(ticket.deletedAt.isNull())
                .fetchOne();

        // Null 값 방지
        int safeDueTodayCount = dueTodayCount != null ? dueTodayCount.intValue() : 0;
        int safeOpenTicketCount = openTicketCount != null ? openTicketCount.intValue() : 0;
        int safeInProgressTicketCount = inProgressTicketCount != null ? inProgressTicketCount.intValue() : 0;
        int safeClosedTicketCount = closedTicketCount != null ? closedTicketCount.intValue() : 0;
        int safeTotalTickets = totalTickets != null ? totalTickets.intValue() : 0;

        String progressExpression = safeTotalTickets > 0
                ? String.format("%d / %d", safeInProgressTicketCount + safeClosedTicketCount, safeTotalTickets)
                : "0 / 0";

        return new TicketProgressResponse(
                safeDueTodayCount,
                safeOpenTicketCount,
                safeInProgressTicketCount,
                safeClosedTicketCount,
                progressExpression
        );
    }

    private Page<Ticket> fetchSearchedTickets(Long memberId, String keyword, int page, int size, String sortByCreatedAt) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.unsorted());

        QTicket ticket = QTicket.ticket;
        BooleanBuilder whereClause = buildWhereClause(memberId, keyword, null, null, null, null, null, null);

        return executeTicketQuery(ticket, whereClause, pageable, sortByCreatedAt);
    }

    private Page<Ticket> fetchTickets(Long memberId, List<Status> statuses, List<String> usernames,
                                      List<String> categories, List<Priority> priorities,
                                      Boolean dueToday, Boolean dueThisWeek, int page, int size, String sortByCreatedAt) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.unsorted());

        QTicket ticket = QTicket.ticket;
        BooleanBuilder whereClause = buildWhereClause(memberId, null, statuses, usernames, categories, priorities, dueToday, dueThisWeek);

        return executeTicketQuery(ticket, whereClause, pageable, sortByCreatedAt);
    }

    private Page<Ticket> executeTicketQuery(QTicket ticket, BooleanBuilder whereClause, Pageable pageable, String sortByCreatedAt) {
        QMember manager = QMember.member;

        OrderSpecifier<?>[] orderSpecifiers = getOrderSpecifiers(ticket, sortByCreatedAt);

        List<Ticket> results = queryFactory
                .selectFrom(ticket)
                .leftJoin(ticket.manager, manager).fetchJoin()
                .where(whereClause)
                .orderBy(orderSpecifiers)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long safeTotalCount = getTotalCount(whereClause);

        return new PageImpl<>(results, pageable, safeTotalCount);
    }

    private BooleanBuilder buildWhereClause(Long memberId, String keyword, List<Status> statuses, List<String> usernames,
                                            List<String> categories, List<Priority> priorities,
                                            Boolean dueToday, Boolean dueThisWeek) {
        QTicket ticket = QTicket.ticket;
        BooleanBuilder whereClause = new BooleanBuilder();
        whereClause.and(ticket.deletedAt.isNull());

        if (memberId != null) {
            whereClause.and(ticket.user.id.eq(memberId));
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

        if (Boolean.TRUE.equals(dueToday) && Boolean.TRUE.equals(dueThisWeek)) {
            throw new ApiException(ErrorCode.INVALID_TICKET_DUE_DATE);
        }

        if (Boolean.TRUE.equals(dueToday)) {
            LocalDate today = LocalDate.now();
            whereClause.and(ticket.dueDate.eq(today));
        }

        if (Boolean.TRUE.equals(dueThisWeek)) {
            LocalDate today = LocalDate.now();
            LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            whereClause.and(ticket.dueDate.between(today, endOfWeek));
        }

        if (keyword != null && !keyword.isBlank()) {
            String searchKeyword = "%" + keyword.toLowerCase() + "%";
            whereClause.and(ticket.title.lower().like(searchKeyword)
                    .or(ticket.content.lower().like(searchKeyword)));
        }

        return whereClause;
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(QTicket ticket, String sortByCreatedAt) {
        if (!"asc".equalsIgnoreCase(sortByCreatedAt) && !"desc".equalsIgnoreCase(sortByCreatedAt)) {
            throw new ApiException(ErrorCode.INVALID_DATA);
        }
        return new OrderSpecifier<?>[]{
                "asc".equalsIgnoreCase(sortByCreatedAt) ? ticket.createdAt.asc() : ticket.createdAt.desc()
        };
    }

    private long getTotalCount(BooleanBuilder whereClause) {
        return Optional.ofNullable(
                queryFactory.select(QTicket.ticket.count())
                        .from(QTicket.ticket)
                        .where(whereClause)
                        .fetchOne()
        ).orElse(0L);
    }

    private void validatePagination(int page, int size, int totalPages) {
        if (page < 1) throw new ApiException(ErrorCode.INVALID_PAGE_NUMBER);
        if (size <= 0) throw new ApiException(ErrorCode.INVALID_PAGE_SIZE);
        if (page > totalPages && totalPages > 0) {
            throw new ApiException(ErrorCode.INVALID_PAGE_NUMBER);
        }
    }
}