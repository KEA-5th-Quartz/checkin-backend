package com.quartz.checkin.repository;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.ticket.response.TicketProgressResponse;
import com.quartz.checkin.entity.Priority;
import com.quartz.checkin.entity.QMember;
import com.quartz.checkin.entity.QTicket;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TicketRepositoryCustomImpl implements TicketRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public TicketProgressResponse getManagerProgress(Long managerId) {
        QTicket ticket = QTicket.ticket;
        LocalDate today = LocalDate.now();

        long dueTodayCount = Optional.ofNullable(queryFactory
                .select(ticket.count())
                .from(ticket)
                .where(ticket.deletedAt.isNull(),
                        ticket.dueDate.eq(today),
                        ticket.manager.id.eq(managerId))
                .fetchOne()).orElse(0L);

        long openTicketCount = Optional.ofNullable(queryFactory
                .select(ticket.count())
                .from(ticket)
                .where(ticket.deletedAt.isNull(),
                        ticket.status.eq(Status.OPEN))
                .fetchOne()).orElse(0L);

        long inProgressTicketCount = Optional.ofNullable(queryFactory
                .select(ticket.count())
                .from(ticket)
                .where(ticket.deletedAt.isNull(),
                        ticket.status.eq(Status.IN_PROGRESS),
                        ticket.manager.id.eq(managerId))
                .fetchOne()).orElse(0L);

        long closedTicketCount = Optional.ofNullable(queryFactory
                .select(ticket.count())
                .from(ticket)
                .where(ticket.deletedAt.isNull(),
                        ticket.status.eq(Status.CLOSED),
                        ticket.manager.id.eq(managerId))
                .fetchOne()).orElse(0L);

        long totalTickets = Optional.ofNullable(queryFactory
                .select(ticket.count())
                .from(ticket)
                .where(ticket.deletedAt.isNull())
                .fetchOne()).orElse(0L);

        String progressExpression = totalTickets > 0
                ? String.format("%d / %d", inProgressTicketCount + closedTicketCount, totalTickets)
                : "0 / 0";

        return new TicketProgressResponse(
                (int) dueTodayCount,
                (int) openTicketCount,
                (int) inProgressTicketCount,
                (int) closedTicketCount,
                progressExpression
        );
    }

    @Override
    public Page<Ticket> fetchSearchedTickets(Long memberId, String keyword, Pageable pageable, String sortByCreatedAt) {
        QTicket ticket = QTicket.ticket;
        BooleanBuilder whereClause = buildWhereClause(memberId, keyword, null, null, null, null, null, null);

        return executeTicketQuery(ticket, whereClause, pageable, sortByCreatedAt);
    }

    @Override
    public Page<Ticket> fetchTickets(Long memberId, List<Status> statuses, List<String> usernames,
                                     List<String> categories, List<Priority> priorities,
                                     Boolean dueToday, Boolean dueThisWeek, Pageable pageable, String sortByCreatedAt) {
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

    @Override
    public long getTotalCount(BooleanBuilder whereClause) {
        return Optional.ofNullable(
                queryFactory.select(QTicket.ticket.count())
                        .from(QTicket.ticket)
                        .where(whereClause)
                        .fetchOne()
        ).orElse(0L);
    }

    @Override
    public int findLastCustomIdByDate(String datePrefix) {
        Integer lastNumber = queryFactory
                .select(QTicket.ticket.customId
                        .substring(QTicket.ticket.customId.length().subtract(3))
                        .stringValue()
                        .castToNum(Integer.class)
                        .max())
                .from(QTicket.ticket)
                .where(QTicket.ticket.customId.startsWith(datePrefix))
                .fetchOne();

        return lastNumber != null ? lastNumber : 0;
    }
}
