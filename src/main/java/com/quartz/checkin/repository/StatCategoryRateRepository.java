package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Map;


public interface StatCategoryRateRepository extends JpaRepository<Member, Long> {
    @Query(value = "SELECT " +
            "    m.username, " +
            "    CONCAT('[', GROUP_CONCAT(" +
            "        CONCAT('{\"name\": \"', c1.name, '\", \"ticket_count\": ', COALESCE(t.ticket_count, 0), '}')" +
            "        ORDER BY c1.name SEPARATOR ', '" +
            "    ), ']') AS state " +
            "FROM member m " +
            "LEFT JOIN (" +
            "    SELECT " +
            "        t.manager_id, " +
            "        COALESCE(c1.category_id, c2.parent_id) AS parent_category_id, " +  // 1차 카테고리 ID 통합
            "        COUNT(t.ticket_id) AS ticket_count " +
            "    FROM ticket t " +
            "    LEFT JOIN category c1 ON t.first_category_id = c1.category_id AND c1.parent_id IS NULL " +  // 1차 카테고리
            "    LEFT JOIN category c2 ON t.first_category_id = c2.category_id AND c2.parent_id IS NOT NULL " +  // 2차 카테고리
            "    WHERE t.status = 'IN_PROGRESS' " +
            "    GROUP BY t.manager_id, parent_category_id " +
            ") t ON m.member_id = t.manager_id " +
            "LEFT JOIN category c1 ON t.parent_category_id = c1.category_id " +  // 1차 카테고리만 JOIN
            "WHERE c1.parent_id IS NULL " +  // 1차 카테고리만 출력
            "AND c1.name IS NOT NULL " +
            "GROUP BY m.member_id, m.username",
            nativeQuery = true)
    List<Map<String, Object>> findStatsByCategory();

}
