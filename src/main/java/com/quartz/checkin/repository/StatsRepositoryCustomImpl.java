package com.quartz.checkin.repository;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.stat.response.StatCategoryCountResponse;
import com.quartz.checkin.dto.stat.response.StatCategoryRateResponse;
import com.quartz.checkin.dto.stat.response.StatClosedRateResponse;
import com.quartz.checkin.dto.stat.response.StatTotalProgressResponse;
import com.quartz.checkin.dto.stat.response.StatTotalProgressResultResponse;
import com.quartz.checkin.entity.QCategory;
import com.quartz.checkin.entity.QMember;
import com.quartz.checkin.entity.QTicket;
import com.quartz.checkin.entity.Status;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StatsRepositoryCustomImpl implements StatsRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<StatCategoryRateResponse> findStatsByCategory() {
        QMember member = QMember.member;
        QTicket ticket = QTicket.ticket;
        QCategory category = QCategory.category;

        List<Tuple> results = queryFactory
                .select(
                        member.username,
                        category.name,
                        ticket.count()
                )
                .from(ticket)
                .join(ticket.manager, member)
                .join(ticket.firstCategory, category)
                .where(
                        ticket.deletedAt.isNull(),
                        member.deletedAt.isNull()
                )
                .groupBy(member.username, category.name)
                .fetch();

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<StatCategoryCountResponse>> groupedData = results.stream()
                .collect(Collectors.groupingBy(
                        tuple -> tuple.get(member.username),
                        Collectors.mapping(tuple -> new StatCategoryCountResponse(
                                tuple.get(category.name),
                                Objects.requireNonNull(tuple.get(ticket.count())).intValue()
                        ), Collectors.toList())
                ));

        return groupedData.entrySet().stream()
                .map(entry -> new StatCategoryRateResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public StatTotalProgressResultResponse findStatTotalProgress() {
        QTicket ticket = QTicket.ticket;
        QMember member = QMember.member;

        LocalDate today = LocalDate.now();
        LocalDate fromDate = today.minusDays(31);
        LocalDate toDate = today.minusDays(1);

        Long overdueCount = queryFactory
                .select(ticket.count())
                .from(ticket)
                .join(member).on(ticket.manager.id.eq(member.id)
                        .and(member.deletedAt.isNull())
                        .and(member.id.ne(-1L)))
                .where(ticket.deletedAt.isNull(),
                        ticket.status.eq(Status.IN_PROGRESS),
                        ticket.dueDate.between(fromDate, toDate))
                .fetchOne();

        List<Tuple> statusCounts = queryFactory
                .select(ticket.status, ticket.count())
                .from(ticket)
                .leftJoin(member).on(ticket.manager.id.eq(member.id)
                        .and(member.deletedAt.isNull()))
                .where(commonTicketConditions(today, fromDate))
                .groupBy(ticket.status)
                .fetch();

        Map<String, Integer> statusTicketCountMap = statusCounts.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(ticket.status).name(),
                        tuple -> tuple.get(ticket.count()).intValue()
                ));

        List<StatTotalProgressResponse> ticketStatusList = List.of(
                new StatTotalProgressResponse("OPEN", statusTicketCountMap.getOrDefault("OPEN", 0)),
                new StatTotalProgressResponse("IN_PROGRESS", statusTicketCountMap.getOrDefault("IN_PROGRESS", 0)),
                new StatTotalProgressResponse("CLOSED", statusTicketCountMap.getOrDefault("CLOSED", 0)),
                new StatTotalProgressResponse("OVERDUE", overdueCount != null ? overdueCount.intValue() : 0)
        );

        int totalTicketCount = ticketStatusList.stream()
                .mapToInt(StatTotalProgressResponse::getTicketCount)
                .sum();

        return new StatTotalProgressResultResponse(totalTicketCount, ticketStatusList);
    }

    @Override
    public List<StatCategoryRateResponse> findStatsByManager(String period) {
        QMember member = QMember.member;
        QTicket ticket = QTicket.ticket;

        LocalDate fromDate = getFromDate(period);

        List<Tuple> results = queryFactory
                .select(
                        member.username,
                        ticket.status,
                        ticket.count()
                )
                .from(ticket)
                .join(ticket.manager, member)
                .where(
                        ticket.deletedAt.isNull(),
                        ticket.createdAt.after(fromDate.atStartOfDay())
                )
                .groupBy(member.username, ticket.status)
                .fetch();

        if (results.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<StatCategoryCountResponse>> groupedData = results.stream()
                .collect(Collectors.groupingBy(
                        tuple -> tuple.get(member.username),
                        Collectors.mapping(tuple -> new StatCategoryCountResponse(
                                tuple.get(ticket.status).name(),
                                Objects.requireNonNull(tuple.get(ticket.count())).intValue()
                        ), Collectors.toList())
                ));

        return groupedData.entrySet().stream()
                .map(entry -> new StatCategoryRateResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public StatClosedRateResponse findClosedRate(String period) {
        QTicket ticket = QTicket.ticket;
        QMember member = QMember.member;

        LocalDate today = LocalDate.now();
        LocalDate fromDate = getFromDate(period);

        Long totalCount = queryFactory
                .select(ticket.count())
                .from(ticket)
                .leftJoin(ticket.manager, member)
                .where(commonTicketConditions(today, fromDate))
                .fetchOne();

        Long closedCount = queryFactory
                .select(ticket.count())
                .from(ticket)
                .leftJoin(ticket.manager, member)
                .where(
                        ticket.deletedAt.isNull(),
                        ticket.status.eq(Status.CLOSED),
                        ticket.createdAt.loe(today.atStartOfDay()),
                        ticket.dueDate.goe(fromDate)
                )
                .fetchOne();

        int total = Objects.requireNonNullElse(totalCount, 0).intValue();
        int closed = Objects.requireNonNullElse(closedCount, 0).intValue();
        int unclosed = total - closed;
        double closedRate = (total > 0) ? ((double) closed / total) * 100 : 0.0;

        return new StatClosedRateResponse(total, closedRate, closed, unclosed);
    }

    private BooleanExpression commonTicketConditions(LocalDate today, LocalDate fromDate) {
        QTicket ticket = QTicket.ticket;
        return ticket.deletedAt.isNull()
                .and(ticket.createdAt.loe(today.atStartOfDay()))
                .and(ticket.dueDate.goe(fromDate));
    }


    private LocalDate getFromDate(String period) {
        return switch (period.toUpperCase()) {
            case "WEEK" -> LocalDate.now().minusWeeks(1);
            case "MONTH" -> LocalDate.now().minusMonths(1);
            case "QUARTER" -> LocalDate.now().minusMonths(3);
            default -> throw new ApiException(ErrorCode.INVALID_STATS_PERIOD_FORMAT);
        };
    }
}
