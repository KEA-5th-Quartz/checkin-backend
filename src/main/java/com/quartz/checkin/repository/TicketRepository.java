package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Ticket;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    // Todo: QueryDSL로 구현

    @Query("""
    SELECT 
        COALESCE(COUNT(CASE WHEN t.dueDate = CURRENT_DATE AND t.manager.id = :managerId THEN 1 END), 0) AS dueTodayCount,
        COALESCE(COUNT(CASE WHEN t.status = 'OPEN' AND t.dueDate >= CURRENT_DATE THEN 1 END), 0) AS openTicketCount,
        COALESCE(COUNT(CASE WHEN t.status = 'IN_PROGRESS' AND t.dueDate >= CURRENT_DATE AND t.manager.id = :managerId THEN 1 END), 0) AS inProgressTicketCount,
        COALESCE(COUNT(CASE WHEN t.status = 'CLOSED' AND t.dueDate >= CURRENT_DATE AND t.manager.id = :managerId THEN 1 END), 0) AS closedTicketCount,
        COALESCE(COUNT(CASE WHEN t.dueDate >= CURRENT_DATE THEN 1 END), 0) AS totalTickets
    FROM Ticket t
    WHERE t.deletedAt IS NULL
""")
    List<Object[]> getManagerTicketStatistics(@Param("managerId") Long managerId);

    @Query("""
    SELECT t.customId 
    FROM Ticket t 
    WHERE t.customId LIKE CONCAT(:prefix, '%') 
    ORDER BY CAST(SUBSTRING(t.customId, LENGTH(:prefix) + 1, 3) AS int) DESC 
    LIMIT 1
""")
    String findLastTicketId(@Param("prefix") String prefix);
}
