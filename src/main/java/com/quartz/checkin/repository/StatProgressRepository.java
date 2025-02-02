package com.quartz.checkin.repository;

import com.quartz.checkin.dto.request.StatProgressRequest;
import com.quartz.checkin.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Map;

public interface StatProgressRepository extends JpaRepository<Ticket, Long> {

    @Query(value = "SELECT " +
            "    m.username AS username, " +
            "    CONCAT( " +
            "        '[', " +
            "        STRING_AGG( " +
            "            CONCAT( " +
            "                '{\"status\": \"', subquery.status, '\", ', " +
            "                '\"ticket_count\": ', subquery.status_count, '}' " +
            "            ), " +
            "            ',' " +
            "            ORDER BY subquery.status DESC " +
            "        ), " +
            "        ']' " +
            "    ) AS state " +
            "FROM " +
            "    ( " +
            "        SELECT " +
            "            t.manager_id, " +
            "            t.status, " +
            "            COUNT(*) AS status_count " +
            "        FROM " +
            "            ticket t " +
            "        WHERE " +
            "            t.status IN ('IN_PROGRESS', 'CLOSED') " +
            "            AND t.deleted_at IS NULL " +
            "            AND ( " +
            "                (:#{#request.type} = 'WEEK' AND t.created_at >= DATEADD('DAY', -7, CURRENT_TIMESTAMP)) OR " +
            "                (:#{#request.type} = 'MONTH' AND t.created_at >= DATEADD('MONTH', -1, CURRENT_TIMESTAMP)) OR " +
            "                (:#{#request.type} = 'QUARTER' AND t.created_at >= DATEADD('MONTH', -3, CURRENT_TIMESTAMP)) " +
            "            ) " +
            "        GROUP BY " +
            "            t.manager_id, t.status " +
            "    ) subquery " +
            "JOIN " +
            "    member m ON subquery.manager_id = m.member_id " +
            "JOIN ( " +
            "    SELECT " +
            "        manager_id, " +
            "        COUNT(*) AS total_count " +
            "    FROM " +
            "        ticket " +
            "    WHERE " +
            "        status IN ('IN_PROGRESS', 'CLOSED') " +
            "        AND deleted_at IS NULL " +
            "        AND ( " +
            "            (:#{#request.type} = 'WEEK' AND created_at >= DATEADD('DAY', -7, CURRENT_TIMESTAMP)) OR " +
            "            (:#{#request.type} = 'MONTH' AND created_at >= DATEADD('MONTH', -1, CURRENT_TIMESTAMP)) OR " +
            "            (:#{#request.type} = 'QUARTER' AND created_at >= DATEADD('MONTH', -3, CURRENT_TIMESTAMP)) " +
            "        ) " +
            "    GROUP BY " +
            "        manager_id " +
            ") total ON subquery.manager_id = total.manager_id " +
            "GROUP BY " +
            "    m.username, total.total_count " +
            "ORDER BY " +
            "    m.username", nativeQuery = true)
    List<Map<String, Object>> findProgressRatesByType(@Param("request") StatProgressRequest request);
}

