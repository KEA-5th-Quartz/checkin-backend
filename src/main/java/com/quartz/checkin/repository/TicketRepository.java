package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    // Todo: QueryDSL로 구현

    @Query("""
    SELECT t.customId 
    FROM Ticket t 
    WHERE t.customId LIKE CONCAT(:prefix, '%') 
    ORDER BY CAST(SUBSTRING(t.customId, LENGTH(:prefix) + 1, 3) AS int) DESC 
    LIMIT 1
""")
    String findLastTicketId(@Param("prefix") String prefix);
}
