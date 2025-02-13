package com.quartz.checkin.repository;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.stat.response.StatCategoryCountResponse;
import com.quartz.checkin.dto.stat.response.StatCategoryRateResponse;
import com.quartz.checkin.dto.stat.response.StatTotalProgressResponse;
import com.quartz.checkin.dto.stat.response.StatTotalProgressResultResponse;
import com.quartz.checkin.entity.QCategory;
import com.quartz.checkin.entity.QMember;
import com.quartz.checkin.entity.QTicket;
import com.quartz.checkin.entity.Status;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.ArrayList;
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

        LocalDate fromDate = LocalDate.now().minusDays(31);
        LocalDate toDate = LocalDate.now().minusDays(1);

        // 연체된 티켓 개수 조회
        Long overdueTicketCount = queryFactory
                .select(ticket.count())
                .from(ticket)
                .join(member).on(ticket.manager.id.eq(member.id)
                        .and(member.deletedAt.isNull())
                        .and(member.id.ne(-1L)))
                .where(ticket.deletedAt.isNull()
                        .and(ticket.status.eq(Status.IN_PROGRESS))
                        .and(ticket.dueDate.between(fromDate, toDate)))
                .fetchOne();

        // 상태별 티켓 개수 조회 (manager_id NULL 값도 포함)
        List<Tuple> statusCounts = queryFactory
                .select(ticket.status, ticket.count())
                .from(ticket)
                .leftJoin(member).on(ticket.manager.id.eq(member.id)
                        .and(member.deletedAt.isNull()))
                .where(ticket.deletedAt.isNull())
                .groupBy(ticket.status)
                .fetch();

        Map<String, Integer> statusTicketCountMap = statusCounts.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(ticket.status).name(),
                        tuple -> tuple.get(ticket.count()).intValue()
                ));

        int overdueCount = overdueTicketCount != null ? overdueTicketCount.intValue() : 0;
        int inProgressCount = statusTicketCountMap.getOrDefault("IN_PROGRESS", 0) - overdueCount;
        inProgressCount = Math.max(inProgressCount, 0);

        List<StatTotalProgressResponse> ticketStatusList = new ArrayList<>();
        ticketStatusList.add(new StatTotalProgressResponse("IN_PROGRESS", inProgressCount));
        ticketStatusList.add(new StatTotalProgressResponse("CLOSED", statusTicketCountMap.getOrDefault("CLOSED", 0)));
        ticketStatusList.add(new StatTotalProgressResponse("OPEN", statusTicketCountMap.getOrDefault("OPEN", 0)));
        ticketStatusList.add(new StatTotalProgressResponse("OVERDUE", overdueCount));

        int totalTicketCount = ticketStatusList.stream()
                .mapToInt(StatTotalProgressResponse::getTicketCount)
                .sum();

        return new StatTotalProgressResultResponse(totalTicketCount, ticketStatusList);
    }

    @Override
    public List<StatCategoryRateResponse> findStatsByManager(String period) {
        QMember member = QMember.member;
        QTicket ticket = QTicket.ticket;

        LocalDate fromDate;
        switch (period.toUpperCase()) {
            case "WEEK":
                fromDate = LocalDate.now().minusWeeks(1);
                break;
            case "MONTH":
                fromDate = LocalDate.now().minusMonths(1);
                break;
            case "QUARTER":
                fromDate = LocalDate.now().minusMonths(3);
                break;
            default:
                throw new ApiException(ErrorCode.INVALID_STATS_PERIOD_FORMAT);
        }

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
                        member.deletedAt.isNull(),
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
}
