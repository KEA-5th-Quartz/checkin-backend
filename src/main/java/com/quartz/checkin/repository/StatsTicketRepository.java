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

    // 각 담당자의 상태별 티켓 수
    @Query(value = """
        SELECT 
            m.username AS userName,
            CONCAT(
                '[', 
                COALESCE(
                    GROUP_CONCAT(
                        CONCAT(
                            '{\"status\": \"', subquery.status, '\", ',
                            '\"ticketCount\": ', subquery.statusCount, '}'
                        ) ORDER BY subquery.status DESC SEPARATOR ','
                    ), ''
                ), 
                ']'
            ) AS state 
        FROM ( 
                SELECT 
                    t.manager_id, 
                    t.status, 
                    COUNT(*) AS statusCount 
                FROM ticket t 
                WHERE t.status IN ('IN_PROGRESS', 'CLOSED') 
                    AND t.deleted_at IS NULL 
                    AND ( 
                        (:#{#request.type} = 'WEEK' AND t.created_at >= DATEADD('DAY', -7, CURRENT_DATE)) OR 
                        (:#{#request.type} = 'MONTH' AND t.created_at >= DATEADD('MONTH', -1, CURRENT_DATE)) OR 
                        (:#{#request.type} = 'QUARTER' AND t.created_at >= DATEADD('MONTH', -3, CURRENT_DATE)) 
                    ) 
                GROUP BY t.manager_id, t.status 
        ) subquery 
        JOIN member m ON subquery.manager_id = m.member_id 
        JOIN ( 
            SELECT 
                manager_id, 
                COUNT(*) AS totalCount 
            FROM ticket 
            WHERE status IN ('IN_PROGRESS', 'CLOSED') 
                AND deleted_at IS NULL 
                AND ( 
                    (:#{#request.type} = 'WEEK' AND created_at >= DATEADD('DAY', -7, CURRENT_DATE)) OR 
                    (:#{#request.type} = 'MONTH' AND created_at >= DATEADD('MONTH', -1, CURRENT_DATE)) OR 
                    (:#{#request.type} = 'QUARTER' AND created_at >= DATEADD('MONTH', -3, CURRENT_DATE)) 
                ) 
            GROUP BY manager_id 
        ) total ON subquery.manager_id = total.manager_id 
        GROUP BY m.username, total.totalCount 
        ORDER BY m.username
        """, nativeQuery = true)
    List<Map<String, Object>> findProgressRatesByType(@Param("request") StatProgressRequest request);

    // 작업완료율 조회
    @Query(value = """
        SELECT 
            CAST(
                COALESCE(SUM(CASE WHEN t.status = 'CLOSED' THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(*), 0), 0) 
                AS DECIMAL(10,2)
            ) AS closed_rate 
        FROM ticket t 
        WHERE t.deleted_at IS NULL 
            AND ( 
                (:#{#request.type} = 'WEEK' AND t.created_at >= DATEADD('DAY', -7, CURRENT_DATE)) OR 
                (:#{#request.type} = 'MONTH' AND t.created_at >= DATEADD('MONTH', -1, CURRENT_DATE)) OR 
                (:#{#request.type} = 'QUARTER' AND t.created_at >= DATEADD('MONTH', -3, CURRENT_DATE)) 
            )
        """, nativeQuery = true)
    Optional<Double> findCompletionRateByType(@Param("request") StatClosedRateRequest request);

    // 카테고리별 티켓 수
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
