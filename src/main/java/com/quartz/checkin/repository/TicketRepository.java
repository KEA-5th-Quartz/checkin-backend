package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Priority;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("SELECT t FROM Ticket t " +
            "WHERE (:statuses IS NULL OR t.status IN :statuses) " +
            "AND (:categories IS NULL OR LOWER(TRIM(t.firstCategory.name)) IN :categories) " +
            "AND (:priorities IS NULL OR t.priority IN :priorities) " +
            "AND (:usernames IS NULL OR t.manager.username IN :usernames OR (t.status = 'OPEN' AND t.manager IS NULL)) " +
            "AND (:dueToday = false OR t.dueDate = CAST(NOW() AS DATE)) " +
            "AND (:dueThisWeek = false OR (t.dueDate BETWEEN CAST(NOW() AS DATE) AND :endOfWeek))")
    Page<Ticket> findManagerTickets(
            @Param("statuses") List<Status> statuses,
            @Param("usernames") List<String> usernames,
            @Param("categories") List<String> categories,
            @Param("priorities") List<Priority> priorities,
            @Param("dueToday") Boolean dueToday,
            @Param("dueThisWeek") Boolean dueThisWeek,
            @Param("endOfWeek") LocalDate endOfWeek, Pageable pageable
    );


    @Query("SELECT t FROM Ticket t " +
            "LEFT JOIN t.manager m " +
            "WHERE (:userId IS NULL OR t.user.id = :userId) " +
            "AND (:statuses IS NULL OR t.status IN :statuses) " +
            "AND (:categories IS NULL OR LOWER(TRIM(t.firstCategory.name)) IN :categories) " +
            "AND (:priorities IS NULL OR t.priority IN :priorities) " +
            "AND (:usernames IS NULL OR m.username IN :usernames) " +
            "AND (:dueToday = false OR t.dueDate = CAST(NOW() AS DATE)) " +
            "AND (:dueThisWeek = false OR (t.dueDate BETWEEN CAST(NOW() AS DATE) AND :endOfWeek))")
    Page<Ticket> findUserTickets(
            @Param("userId") Long userId,
            @Param("statuses") List<Status> statuses,
            @Param("usernames") List<String> usernames,
            @Param("categories") List<String> categories,
            @Param("priorities") List<Priority> priorities,
            @Param("dueToday") Boolean dueToday,
            @Param("dueThisWeek") Boolean dueThisWeek,
            @Param("endOfWeek") LocalDate endOfWeek, Pageable pageable
    );

    @Query("SELECT t FROM Ticket t LEFT JOIN FETCH t.manager " +
            "WHERE (:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(t.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Ticket> searchTickets(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT t FROM Ticket t " +
            "WHERE t.user.id = :memberId " +
            "AND (:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(t.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Ticket> searchMyTickets(@Param("memberId") Long memberId, @Param("keyword") String keyword, Pageable pageable);
}
