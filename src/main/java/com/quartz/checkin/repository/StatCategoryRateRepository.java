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
            "        CONCAT('{\"name\": \"', c.name, '\", \"ticket_count\": ', COALESCE(t.ticket_count, 0), '}')" +
            "    ), ']') AS state " +
            "FROM " +
            "    member m " +
            "LEFT JOIN ( " +
            "    SELECT " +
            "        t.manager_id, " +
            "        t.first_category_id, " +
            "        COUNT(t.ticket_id) AS ticket_count " +
            "    FROM " +
            "        ticket t " +
            "    WHERE " +
            "        t.status = 'IN_PROGRESS' " + // 진행중 상태의 티켓만 조회
            "    GROUP BY " +
            "        t.manager_id, t.first_category_id " +
            ") t ON m.member_id = t.manager_id " +
            "LEFT JOIN " +
            "    category c ON t.first_category_id = c.parent_id " +
            "WHERE " +
            "    c.name IS NOT NULL " +
            "GROUP BY " +
            "    m.member_id", nativeQuery = true)
    List<Map<String, Object>> findStatsByCategory();

}
