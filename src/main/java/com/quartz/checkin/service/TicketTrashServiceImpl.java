package com.quartz.checkin.service;

import static com.quartz.checkin.common.PaginationUtils.validatePagination;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.converter.TicketResponseConverter;
import com.quartz.checkin.dto.ticket.response.SoftDeletedTicketResponse;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.repository.TicketRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketTrashServiceImpl implements TicketTrashService {
    private final MemberService memberService;
    private final TicketRepository ticketRepository;

    @Override
    public void restoreTickets(Long memberId, List<Long> ticketIds) {
        List<Ticket> tickets = findAndValidateTickets(memberId, ticketIds);

        for (Ticket ticket : tickets) {
            ticket.restoreTicket();
        }

        ticketRepository.saveAll(tickets);
    }

    @Override
    public void deleteTickets(Long memberId, List<Long> ticketIds) {
        List<Ticket> tickets = findAndValidateTickets(memberId, ticketIds);
        ticketRepository.deleteAll(tickets);
    }

    @Override
    public SoftDeletedTicketResponse getDeletedTickets(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Ticket> ticketPage = ticketRepository.fetchDeletedTickets(memberId, pageable);
        validatePagination(page, size, ticketPage.getTotalPages());
        return TicketResponseConverter.toSoftDeletedTicketResponse(ticketPage);
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

    @Scheduled(cron = "0 0 3 * * ?")
    public void deleteExpiredTickets() {
        LocalDate thresholdDate = LocalDate.now().minusDays(7);
        List<Ticket> expiredTickets = ticketRepository.findExpiredTickets(thresholdDate);

        if (!expiredTickets.isEmpty()) {
            expiredTickets.forEach(Ticket::softDelete);
            ticketRepository.saveAll(expiredTickets);
        }
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void softDeleteOldClosedTickets() {
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
        List<Ticket> oldTickets = ticketRepository.findOldClosedTickets(sixMonthsAgo);

        if (!oldTickets.isEmpty()) {
            oldTickets.forEach(Ticket::softDelete);
            ticketRepository.saveAll(oldTickets);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void deleteOldSoftDeletedTickets() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Ticket> expiredTickets = ticketRepository.findOldSoftDeletedTickets(thirtyDaysAgo);
        ticketRepository.deleteAll(expiredTickets);
    }
}
