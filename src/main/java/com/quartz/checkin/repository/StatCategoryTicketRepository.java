package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StatCategoryTicketRepository extends JpaRepository<Ticket, Long> {

    @Query(value = "SELECT c.name AS name, COUNT(t.ticket_id) AS ticket_count " +
            "FROM category c " +
            "LEFT JOIN ticket t ON c.category_id = t.first_category_id AND t.status = 'IN_PROGRESS' " +
            "WHERE c.parent_id IS NULL " +
            "GROUP BY c.category_id, c.name " +
            "ORDER BY ticket_count DESC", nativeQuery = true)
    List<Object[]> findCategoryTicketStats();
}
