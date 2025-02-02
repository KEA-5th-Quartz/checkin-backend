package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface StatTotalProgressRepository extends JpaRepository<Ticket, Long> {

    @Query(value = "SELECT " +
            "    (SELECT COUNT(*) FROM ticket WHERE deleted_at IS NOT NULL) AS deleted_count, " +
            "    CONCAT( " +
            "        '[', " +
            "        GROUP_CONCAT( " +
            "            CONCAT( " +
            "                '{\"status\": \"', status, '\", ', " +
            "                '\"ticket_count\": ', ticket_count, '}' " +
            "            ) " +
            "            ORDER BY status SEPARATOR ',' " +
            "        ), " +
            "        ']' " +
            "    ) AS state_json " +
            "FROM ( " +
            "    SELECT " +
            "        status, " +
            "        COUNT(*) AS ticket_count " +
            "    FROM " +
            "        ticket " +
            "    WHERE " +
            "        deleted_at IS NULL " +
            "    GROUP BY " +
            "        status " +
            ") AS status_counts", nativeQuery = true)
    List<Object[]> findStatTotalProgress();
}