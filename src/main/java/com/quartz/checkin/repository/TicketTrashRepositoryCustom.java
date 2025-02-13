package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Ticket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TicketTrashRepositoryCustom {
    List<Ticket> findExpiredTickets(LocalDate thresholdDate);
    List<Ticket> findOldClosedTickets(LocalDate sixMonthsAgo);
    Page<Ticket> fetchDeletedTickets(Long memberId, Pageable pageable);
    List<Ticket> findOldSoftDeletedTickets(LocalDateTime thirtyDaysAgo);
}
