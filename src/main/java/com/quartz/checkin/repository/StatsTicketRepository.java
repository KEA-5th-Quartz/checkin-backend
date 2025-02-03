package com.quartz.checkin.repository;


import com.quartz.checkin.dto.statisitics.request.StatClosedRateRequest;
import com.quartz.checkin.dto.statisitics.request.StatProgressRequest;
import com.quartz.checkin.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface StatsTicketRepository extends JpaRepository<Ticket, Long> {

    // 1. 전체 진행 상태 통계 조회
    @Query(value = """
        SELECT 
            (SELECT COUNT(*) FROM ticket WHERE deleted_at IS NOT NULL) AS deleted_count,
            CONCAT(
                '[',
                GROUP_CONCAT(
                    CONCAT(
                        '{\"status\": \"', status, '\", ',
                        '\"ticket_count\": ', ticket_count, '}'
                    )
                    ORDER BY status SEPARATOR ','
                ),
                ']'
            ) AS state_json 
        FROM ( 
            SELECT 
                status, 
                COUNT(*) AS ticket_count 
            FROM 
                ticket 
            WHERE 
                deleted_at IS NULL 
            GROUP BY 
                status 
        ) AS status_counts
        """, nativeQuery = true)
    List<Object[]> findStatTotalProgress();

    // 2. 유형별 진행률 조회
    @Query(value = """
        SELECT 
            m.username AS username,
            CONCAT(
                '[',
                STRING_AGG(
                    CONCAT(
                        '{\"status\": \"', subquery.status, '\", ',
                        '\"ticket_count\": ', subquery.status_count, '}'
                    ),
                    ',' 
                    ORDER BY subquery.status DESC 
                ),
                ']'
            ) AS state 
        FROM 
            ( 
                SELECT 
                    t.manager_id, 
                    t.status, 
                    COUNT(*) AS status_count 
                FROM 
                    ticket t 
                WHERE 
                    t.status IN ('IN_PROGRESS', 'CLOSED') 
                    AND t.deleted_at IS NULL 
                    AND ( 
                        (:#{#request.type} = 'WEEK' AND t.created_at >= DATEADD('DAY', -7, CURRENT_TIMESTAMP)) OR 
                        (:#{#request.type} = 'MONTH' AND t.created_at >= DATEADD('MONTH', -1, CURRENT_TIMESTAMP)) OR 
                        (:#{#request.type} = 'QUARTER' AND t.created_at >= DATEADD('MONTH', -3, CURRENT_TIMESTAMP)) 
                    ) 
                GROUP BY 
                    t.manager_id, t.status 
            ) subquery 
        JOIN 
            member m ON subquery.manager_id = m.member_id 
        JOIN ( 
            SELECT 
                manager_id, 
                COUNT(*) AS total_count 
            FROM 
                ticket 
            WHERE 
                status IN ('IN_PROGRESS', 'CLOSED') 
                AND deleted_at IS NULL 
                AND ( 
                    (:#{#request.type} = 'WEEK' AND created_at >= DATEADD('DAY', -7, CURRENT_TIMESTAMP)) OR 
                    (:#{#request.type} = 'MONTH' AND created_at >= DATEADD('MONTH', -1, CURRENT_TIMESTAMP)) OR 
                    (:#{#request.type} = 'QUARTER' AND created_at >= DATEADD('MONTH', -3, CURRENT_TIMESTAMP)) 
                ) 
            GROUP BY 
                manager_id 
        ) total ON subquery.manager_id = total.manager_id 
        GROUP BY 
            m.username, total.total_count 
        ORDER BY 
            m.username
        """, nativeQuery = true)
    List<Map<String, Object>> findProgressRatesByType(@Param("request") StatProgressRequest request);

    // 3. 티켓 완료율 조회
    @Query(value = """
        SELECT 
            ROUND(
                SUM(CASE WHEN t.status = 'CLOSED' THEN 1 ELSE 0 END) * 1.0 / COUNT(*) * 100,
                2 
            ) AS closed_rate 
        FROM 
            ticket t 
        WHERE 
            t.deleted_at IS NULL 
            AND ( 
                (:#{#request.type} = 'WEEK' AND t.created_at >= DATEADD('DAY', -7, CURRENT_TIMESTAMP)) OR 
                (:#{#request.type} = 'MONTH' AND t.created_at >= DATEADD('MONTH', -1, CURRENT_TIMESTAMP)) OR 
                (:#{#request.type} = 'QUARTER' AND t.created_at >= DATEADD('MONTH', -3, CURRENT_TIMESTAMP)) 
            )
        """, nativeQuery = true)
    Optional<Double> findCompletionRateByType(@Param("request") StatClosedRateRequest request);

    // 4. 카테고리별 티켓 통계 조회
    @Query(value = """
        SELECT c.name AS name, COUNT(t.ticket_id) AS ticket_count 
        FROM category c 
        LEFT JOIN ticket t ON c.category_id = t.first_category_id AND t.status = 'IN_PROGRESS'
        WHERE c.parent_id IS NULL 
        GROUP BY c.category_id, c.name 
        ORDER BY ticket_count DESC
        """, nativeQuery = true)
    List<Object[]> findCategoryTicketStats();
}