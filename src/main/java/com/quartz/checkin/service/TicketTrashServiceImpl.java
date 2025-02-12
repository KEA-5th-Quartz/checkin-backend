package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.converter.TicketResponseConverter;
import com.quartz.checkin.dto.ticket.response.SoftDeletedTicketResponse;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.QMember;
import com.quartz.checkin.entity.QTicket;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.repository.TicketRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketTrashServiceImpl implements TicketTrashService {
    private final MemberService memberService;
    private final TicketRepository ticketRepository;
    private final JPAQueryFactory queryFactory;

    private static final QTicket ticket = QTicket.ticket;
    private static final QMember manager = new QMember("manager");


    @Transactional
    @Override
    public void restoreTickets(Long memberId, List<Long> ticketIds) {
        List<Ticket> tickets = findAndValidateTickets(memberId, ticketIds);

        for (Ticket ticket : tickets) {
            ticket.restoreTicket();
        }

        ticketRepository.saveAll(tickets);
    }

    @Transactional
    @Override
    public void deleteTickets(Long memberId, List<Long> ticketIds) {
        List<Ticket> tickets = findAndValidateTickets(memberId, ticketIds);

        ticketRepository.deleteAll(tickets);
    }

    private List<Ticket> findAndValidateTickets(Long memberId, List<Long> ticketIds) {
        Member member = memberService.getMemberByIdOrThrow(memberId);

        List<Ticket> tickets = ticketRepository.findAllById(ticketIds);

        if (tickets.size() != ticketIds.size()) {
            throw new ApiException(ErrorCode.TICKET_NOT_FOUND);
        }

        for (Ticket ticket : tickets) {
            if (!ticket.getUser().getId().equals(member.getId())) {
                throw new ApiException(ErrorCode.FORBIDDEN);
            }
        }

        return tickets;
    }

    @Transactional
    @Scheduled(cron = "0 0 3 * * ?")
    public void deleteExpiredTickets() {
        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.minusDays(7);

        QTicket ticket = QTicket.ticket;

        List<Ticket> expiredTickets = queryFactory
                .selectFrom(ticket)
                .where(
                        ticket.status.eq(Status.OPEN),
                        ticket.dueDate.loe(thresholdDate),
                        ticket.deletedAt.isNull()
                )
                .fetch();

        if (!expiredTickets.isEmpty()) {
            expiredTickets.forEach(Ticket::softDelete);
            ticketRepository.saveAll(expiredTickets);
        }
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * ?")
    public void softDeleteOldClosedTickets() {
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);

        List<Ticket> oldTickets = queryFactory
                .selectFrom(ticket)
                .where(ticket.status.eq(Status.CLOSED)
                        .and(ticket.dueDate.before(sixMonthsAgo))
                        .and(ticket.deletedAt.isNull()))
                .fetch();

        if (!oldTickets.isEmpty()) {
            oldTickets.forEach(Ticket::softDelete);
            ticketRepository.saveAll(oldTickets);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public SoftDeletedTicketResponse getDeletedTickets(Long memberId, int page, int size) {
        Page<Ticket> ticketPage = fetchDeletedTickets(memberId, page, size);
        validatePagination(page, size, ticketPage.getTotalPages());
        return TicketResponseConverter.toSoftDeletedTicketResponse(ticketPage);
    }

    private Page<Ticket> fetchDeletedTickets(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);

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

    @Transactional
    @Scheduled(cron = "0 0 2 * * ?")
    public void deleteOldSoftDeletedTickets() {
        QTicket ticket = QTicket.ticket;
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<Ticket> expiredTickets = queryFactory
                .selectFrom(ticket)
                .where(ticket.deletedAt.isNotNull()
                        .and(ticket.deletedAt.before(thirtyDaysAgo)))
                .fetch();
        ticketRepository.deleteAll(expiredTickets);
    }


    private void validatePagination(int page, int size, int totalPages) {
        if (page < 1) throw new ApiException(ErrorCode.INVALID_PAGE_NUMBER);
        if (size <= 0) {
            throw new ApiException(ErrorCode.INVALID_PAGE_SIZE);
        }
        if (page > totalPages && totalPages > 0) {
            throw new ApiException(ErrorCode.INVALID_PAGE_NUMBER);
        }
    }
}
