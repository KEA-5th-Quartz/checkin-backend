package com.quartz.checkin.repository;


import com.quartz.checkin.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface StatsMemberRepository extends JpaRepository<Member, Long> {

    // 각 담당자의 카테고리별 티켓수
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
            COALESCE(c1.category_id, c2.parent_id) AS parentCategoryId,  -- 별칭 정의
            COUNT(t.ticket_id) AS ticketCount  -- 소문자로 통일
        FROM ticket t 
        LEFT JOIN category c1 
            ON t.first_category_id = c1.category_id AND c1.parent_id IS NULL 
        LEFT JOIN category c2 
            ON t.first_category_id = c2.category_id AND c2.parent_id IS NOT NULL 
        WHERE t.status = 'IN_PROGRESS' 
        GROUP BY t.manager_id, COALESCE(c1.category_id, c2.parent_id)  -- MySQL에서 별칭 사용 불가
    ) t ON m.member_id = t.manager_id 
    LEFT JOIN category c1 
        ON t.parentCategoryId = c1.category_id 
    WHERE c1.parent_id IS NULL 
        AND c1.name IS NOT NULL 
    GROUP BY m.member_id, m.username
    """, nativeQuery = true)
    List<Map<String, Object>> findStatsByCategory();
    // 전체작업상태 분포(OVERDUE 포함)
    @Query(value = """
    SELECT 
        -- OVERDUE 계산 (IN_PROGRESS 상태면서 31일 전부터 어제까지 기한이 지난 티켓 수)
        (SELECT COUNT(*) 
         FROM ticket 
         WHERE deleted_at IS NULL 
         AND status = 'IN_PROGRESS'
         AND due_date BETWEEN DATE_SUB(CURRENT_DATE(), INTERVAL 31 DAY) AND DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY)
        ) AS overdue,
        
        -- 상태별 티켓 개수를 JSON 형식으로 변환
        CONCAT(
            '[', 
            GROUP_CONCAT(
                CONCAT(
                    '{\"status\": \"', status, '\", ',
                    '\"ticketCount\": ', ticketCount, '}'
                ) 
                ORDER BY status SEPARATOR ','
            ), 
            ']'
        ) AS state 
    FROM ( 
        SELECT 
            status, 
            COUNT(*) AS ticketCount 
        FROM 
            ticket 
        WHERE 
            deleted_at IS NULL 
        GROUP BY 
            status 
    ) AS statusCounts
    """, nativeQuery = true)
    List<Object[]> findStatTotalProgress();
}