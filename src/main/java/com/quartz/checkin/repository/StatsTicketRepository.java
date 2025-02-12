package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Repository
public interface StatsTicketRepository extends JpaRepository<Ticket, Long> {

    // 각 담당자의 상태별 티켓 수 (soft/hard delete된 담당자 제외)
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
            JOIN member m ON t.manager_id = m.member_id  
                AND m.deleted_at IS NULL  
                AND m.member_id != -1    
            WHERE t.status IN ('IN_PROGRESS', 'CLOSED') 
                AND t.deleted_at IS NULL  
                AND ( 
                    (:type = 'WEEK' AND t.created_at >= DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY)) OR 
                    (:type = 'MONTH' AND t.created_at >= DATE_SUB(CURRENT_DATE(), INTERVAL 1 MONTH)) OR 
                    (:type = 'QUARTER' AND t.created_at >= DATE_SUB(CURRENT_DATE(), INTERVAL 3 MONTH)) 
                ) 
            GROUP BY t.manager_id, t.status 
    ) subquery 
    JOIN member m ON subquery.manager_id = m.member_id 
        AND m.deleted_at IS NULL   
        AND m.member_id != -1   
    JOIN ( 
        SELECT 
            t.manager_id, 
            COUNT(*) AS totalCount 
        FROM ticket t 
        JOIN member m ON t.manager_id = m.member_id  
            AND m.deleted_at IS NULL 
            AND m.member_id != -1  
        WHERE t.status IN ('IN_PROGRESS', 'CLOSED') 
            AND t.deleted_at IS NULL 
            AND ( 
                (:type = 'WEEK' AND t.created_at >= DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY)) OR 
                (:type = 'MONTH' AND t.created_at >= DATE_SUB(CURRENT_DATE(), INTERVAL 1 MONTH)) OR 
                (:type = 'QUARTER' AND t.created_at >= DATE_SUB(CURRENT_DATE(), INTERVAL 3 MONTH)) 
            ) 
        GROUP BY t.manager_id 
    ) total ON subquery.manager_id = total.manager_id 
    GROUP BY m.username, total.totalCount 
    ORDER BY m.username
    """, nativeQuery = true)
    List<Map<String, Object>> findProgressRatesByType(@Param("type") String type);

    // 작업완료율 조회 (soft/hard delete된 담당자 제외)
    @Query(value = """
    SELECT 
        CAST(
            COALESCE(SUM(CASE WHEN t.status = 'CLOSED' THEN 1 ELSE 0 END) * 100.0 / NULLIF(COUNT(*), 0), 0) 
            AS DECIMAL(10,2)
        ) AS closed_rate 
    FROM ticket t 
    JOIN member m ON t.manager_id = m.member_id 
        AND m.deleted_at IS NULL 
        AND m.member_id != -1   
    WHERE t.deleted_at IS NULL  
        AND ( 
            (:type = 'WEEK' AND t.created_at >= DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY)) OR 
            (:type = 'MONTH' AND t.created_at >= DATE_SUB(CURRENT_DATE(), INTERVAL 1 MONTH)) OR 
            (:type = 'QUARTER' AND t.created_at >= DATE_SUB(CURRENT_DATE(), INTERVAL 3 MONTH)) 
        )
    """, nativeQuery = true)
    Optional<Double> findCompletionRateByType(@Param("type") String type);

    // 카테고리별 티켓 수 (soft/hard delete된 담당자 제외)
    @Query(value = """
    SELECT c.name AS name, COUNT(t.ticket_id) AS ticket_count 
    FROM category c 
    LEFT JOIN ticket t ON c.category_id = t.first_category_id 
        AND t.status = 'IN_PROGRESS' 
        AND t.deleted_at IS NULL  
        AND t.manager_id NOT IN (  
            SELECT m.member_id FROM member m 
            WHERE m.deleted_at IS NOT NULL OR m.member_id = -1
        )
    WHERE c.parent_id IS NULL 
    GROUP BY c.name 
    ORDER BY ticket_count DESC
    """, nativeQuery = true)
    List<Object[]> findCategoryTicketStats();
}
