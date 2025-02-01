package com.quartz.checkin.repository;

import com.quartz.checkin.dto.request.StatClosedRateRequest;
import com.quartz.checkin.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface StatClosedRateRepository extends JpaRepository<Ticket, Long> {

    @Query(value = "SELECT " +
            "    ROUND( " +
            "        SUM(CASE WHEN t.status = 'CLOSED' THEN 1 ELSE 0 END) * 1.0 / COUNT(*) * 100, " +
            "        2 " +
            "    ) AS closed_rate " +
            "FROM " +
            "    ticket t " +
            "WHERE " +
            "    t.deleted_at IS NULL " +
            "    AND ( " +
            "        (:#{#request.type} = 'WEEK' AND t.created_at >= DATEADD('DAY', -7, CURRENT_TIMESTAMP)) OR " +
            "        (:#{#request.type} = 'MONTH' AND t.created_at >= DATEADD('MONTH', -1, CURRENT_TIMESTAMP)) OR " +
            "        (:#{#request.type} = 'QUARTER' AND t.created_at >= DATEADD('MONTH', -3, CURRENT_TIMESTAMP)) " +
            "    )", nativeQuery = true)
    Optional<Double> findCompletionRateByType(@Param("request") StatClosedRateRequest request);
}