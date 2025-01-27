package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("SELECT t FROM Ticket t " +
            "LEFT JOIN t.manager m " +
            "WHERE (:status IS NULL OR t.status = :status) " +
            "AND (:category IS NULL OR LOWER(TRIM(t.firstCategory.name)) LIKE LOWER(CONCAT('%', :category, '%')))" +
            "AND (:username IS NULL OR t.manager.username = :username OR (t.status = 'OPEN' AND t.manager IS NULL))")
    Page<Ticket> findTickets(Status status, String username, String category, Pageable pageable);

    @Query("SELECT t FROM Ticket t")
    Page<Ticket> findAllTickets(Pageable pageable);
}
