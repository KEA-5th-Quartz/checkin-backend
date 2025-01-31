package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

public interface StatProgressRepository extends JpaRepository<Ticket, Long> {

    @Query(value = "SELECT " +
            "    m.username AS username, " +
            "    subquery.status AS status, " +
            "    ROUND(subquery.ticket_count / subquery.total_ticket_count * 100, 2) AS rate " +
            "FROM " +
            "    ( " +
            "        SELECT " +
            "            t.manager_id, " +
            "            t.status, " +
            "            COUNT(*) AS ticket_count, " +
            "            COUNT(*) OVER (PARTITION BY t.manager_id) AS total_ticket_count " +
            "        FROM " +
            "            ticket t " +
            "        WHERE " +
            "            t.status IN ('In Progress', 'Closed') " +
            "            AND ( " +
            "                CASE " +
            "                    WHEN :type = 'WEEK' THEN YEARWEEK(t.created_at) = YEARWEEK(NOW()) " +
            "                    WHEN :type = 'MONTH' THEN DATE_FORMAT(t.created_at, '%Y-%m') = DATE_FORMAT(NOW(), '%Y-%m') " +
            "                    WHEN :type = 'QUARTER' THEN QUARTER(t.created_at) = QUARTER(NOW()) AND YEAR(t.created_at) = YEAR(NOW()) " +
            "                    ELSE 1=1 " +
            "                END " +
            "            ) " +
            "        GROUP BY " +
            "            t.manager_id, t.status " +
            "    ) AS subquery " +
            "JOIN " +
            "    member m " +
            "ON " +
            "    subquery.manager_id = m.member_id " +
            "ORDER BY " +
            "    m.username, subquery.status", nativeQuery = true)
    List<Map<String, Object>> findProgressRatesByType(@RequestBody String type);
}
