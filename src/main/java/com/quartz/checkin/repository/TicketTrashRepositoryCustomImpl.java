package com.quartz.checkin.repository;

import com.quartz.checkin.entity.QMember;
import com.quartz.checkin.entity.QTicket;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TicketTrashRepositoryCustomImpl implements TicketTrashRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QTicket ticket = QTicket.ticket;
    private static final QMember manager = new QMember("manager");

    @Override
    public List<Ticket> findExpiredTickets(LocalDate thresholdDate) {
        return queryFactory
                .selectFrom(ticket)
                .where(
                        ticket.status.eq(Status.OPEN),
                        ticket.dueDate.loe(thresholdDate),
                        ticket.deletedAt.isNull()
                )
                .fetch();
    }

    @Override
    public List<Ticket> findOldClosedTickets(LocalDate sixMonthsAgo) {
        return queryFactory
                .selectFrom(ticket)
                .where(ticket.status.eq(Status.CLOSED)
                        .and(ticket.dueDate.before(sixMonthsAgo))
                        .and(ticket.deletedAt.isNull()))
                .fetch();
    }

    @Override
    public Page<Ticket> fetchDeletedTickets(Long memberId, Pageable pageable) {
        List<Ticket> ticketList = queryFactory
                .selectFrom(ticket)
                .leftJoin(ticket.manager, manager).fetchJoin()
                .where(ticket.deletedAt.isNotNull()
                        .and(ticket.user.id.eq(memberId)))
                .orderBy(ticket.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long totalCount = Optional.ofNullable(queryFactory
                .select(ticket.count())
                .from(ticket)
                .where(ticket.deletedAt.isNotNull()
                        .and(ticket.user.id.eq(memberId)))
                .fetchOne()).orElse(0L);

        return new PageImpl<>(ticketList, pageable, totalCount);
    }

    @Override
    public List<Ticket> findOldSoftDeletedTickets(LocalDateTime thirtyDaysAgo) {
        return queryFactory
                .selectFrom(ticket)
                .where(ticket.deletedAt.isNotNull()
                        .and(ticket.deletedAt.before(thirtyDaysAgo)))
                .fetch();
    }
}
