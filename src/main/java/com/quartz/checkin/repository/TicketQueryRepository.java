package com.quartz.checkin.repository;

import com.quartz.checkin.entity.QTicket;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TicketQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<Ticket> findTicketsToDelete(LocalDate thresholdDate) {
        QTicket ticket = QTicket.ticket;

        return queryFactory
                .selectFrom(ticket)
                .where(
                        ticket.status.eq(Status.OPEN),
                        ticket.dueDate.loe(thresholdDate),
                        ticket.deletedAt.isNull()
                )
                .fetch();
    }
}
