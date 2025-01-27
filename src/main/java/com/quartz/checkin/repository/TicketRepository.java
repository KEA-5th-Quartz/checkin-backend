package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Priority;
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
            "AND (:priority IS NULL OR t.priority = :priority)" +
            "AND (:username IS NULL OR t.manager.username = :username OR (t.status = 'OPEN' AND t.manager IS NULL))")
    Page<Ticket> findTickets(Status status, String username, String category, Priority priority, Pageable pageable);

    @Query("SELECT t FROM Ticket t")
    Page<Ticket> findAllTickets(Pageable pageable);

    @Query("SELECT t FROM Ticket t " +
            "LEFT JOIN t.manager m " +
            "WHERE t.user.id = :userId " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:category IS NULL OR LOWER(TRIM(t.firstCategory.name)) = LOWER(TRIM(:category))) " +
            "AND (:username IS NULL OR m.username = :username) ")
    Page<Ticket> findUserTickets(Long userId, Status status, String username, String category, Pageable pageable);
}
