package com.quartz.checkin.repository;


import com.quartz.checkin.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface StatsMemberRepository extends JpaRepository<Member, Long> {

    // 1. due-today
    @Query(value = """
        SELECT m.username, COALESCE(COUNT(t.ticket_id), 0) AS ticket_count 
        FROM member m 
        LEFT JOIN ticket t ON m.member_id = t.manager_id 
        AND t.status = 'IN_PROGRESS' 
        AND t.due_date = CURDATE() 
        WHERE m.member_id = :managerId 
        GROUP BY m.member_id, m.username
        """, nativeQuery = true)
    List<Map<String, Object>> findTicketCountByManagerId(@Param("managerId") Long managerId);

    // 2. 카테고리별 진행률 조회
    @Query(value = """
        SELECT 
            m.username, 
            CONCAT('[', GROUP_CONCAT(
                CONCAT('{\"name\": \"', c1.name, '\", \"ticket_count\": ', COALESCE(t.ticket_count, 0), '}')
                ORDER BY c1.name SEPARATOR ', '
            ), ']') AS state 
        FROM member m 
        LEFT JOIN (
            SELECT 
                t.manager_id, 
                COALESCE(c1.category_id, c2.parent_id) AS parent_category_id, 
                COUNT(t.ticket_id) AS ticket_count 
            FROM ticket t 
            LEFT JOIN category c1 ON t.first_category_id = c1.category_id AND c1.parent_id IS NULL 
            LEFT JOIN category c2 ON t.first_category_id = c2.category_id AND c2.parent_id IS NOT NULL 
            WHERE t.status = 'IN_PROGRESS' 
            GROUP BY t.manager_id, parent_category_id 
        ) t ON m.member_id = t.manager_id 
        LEFT JOIN category c1 ON t.parent_category_id = c1.category_id 
        WHERE c1.parent_id IS NULL 
        AND c1.name IS NOT NULL 
        GROUP BY m.member_id, m.username
        """, nativeQuery = true)
    List<Map<String, Object>> findStatsByCategory();
}