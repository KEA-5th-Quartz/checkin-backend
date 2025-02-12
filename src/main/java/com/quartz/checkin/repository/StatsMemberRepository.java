package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.List;

@Repository
public interface StatsMemberRepository extends JpaRepository<Member, Long> {

    // 각 담당자의 카테고리별 티켓수 (soft/hard delete 제외)
    @Query(value = """
    SELECT 
        m.username, 
        CONCAT('[', 
            GROUP_CONCAT(
                CONCAT('{\"categoryName\": \"', c1.name, '\", \"ticketCount\": ', COALESCE(ticketCount, 0), '}') 
                ORDER BY c1.name SEPARATOR ', '
            ), 
        ']') AS state 
    FROM member m 
    LEFT JOIN (
        SELECT 
            t.manager_id, 
            COALESCE(c1.category_id, c2.parent_id) AS parentCategoryId,
            COUNT(t.ticket_id) AS ticketCount  
        FROM ticket t 
        JOIN member m ON t.manager_id = m.member_id 
            AND m.deleted_at IS NULL 
            AND m.member_id != -1
        LEFT JOIN category c1 
            ON t.first_category_id = c1.category_id AND c1.parent_id IS NULL 
        LEFT JOIN category c2 
            ON t.first_category_id = c2.category_id AND c2.parent_id IS NOT NULL 
        WHERE t.status = 'IN_PROGRESS' 
            AND t.deleted_at IS NULL 
        GROUP BY t.manager_id, COALESCE(c1.category_id, c2.parent_id) 
    ) t ON m.member_id = t.manager_id 
    LEFT JOIN category c1 
        ON t.parentCategoryId = c1.category_id 
    WHERE c1.parent_id IS NULL 
        AND c1.name IS NOT NULL 
        AND m.deleted_at IS NULL  
        AND m.member_id != -1
    GROUP BY m.member_id, m.username
    """, nativeQuery = true)
    List<Map<String, Object>> findStatsByCategory();

    // 전체작업상태 분포(OVERDUE 포함) (soft/hard delete 제외)
    @Query(value = """
    SELECT 
        (SELECT COUNT(*) 
         FROM ticket t 
         JOIN member m ON t.manager_id = m.member_id  
             AND m.deleted_at IS NULL  
             AND m.member_id != -1     
         WHERE t.deleted_at IS NULL  
         AND t.status = 'IN_PROGRESS'
         AND t.due_date BETWEEN DATE_SUB(CURRENT_DATE(), INTERVAL 31 DAY) 
                             AND DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY)
        ) AS overdue,

        CONCAT(
            '[', 
            (SELECT GROUP_CONCAT(
                CONCAT(
                    '{"status": "', t2.status, '", ',
                    '"ticketCount": ', t2.ticket_count, '}'  
                ) ORDER BY t2.status SEPARATOR ','
            ) 
            FROM (SELECT t.status, COUNT(*) AS ticket_count  
                  FROM ticket t
                  JOIN member m ON t.manager_id = m.member_id  
                      AND m.deleted_at IS NULL  
                      AND m.member_id != -1  
                  WHERE t.deleted_at IS NULL  
                  GROUP BY t.status) t2
            ),
            ']'
        ) AS state 
    FROM DUAL
    """, nativeQuery = true)
    List<Object[]> findStatTotalProgress();

}