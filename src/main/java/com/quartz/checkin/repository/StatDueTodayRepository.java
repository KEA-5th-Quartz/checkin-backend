package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Map;

public interface StatDueTodayRepository extends Repository<Member, Long> {

    @Query(value = "SELECT m.username, COALESCE(COUNT(t.ticket_id), 0) AS ticket_count " +
            "FROM member m " +
            "LEFT JOIN ticket t ON m.member_id = t.manager_id " +
            "AND t.status = 'IN_PROGRESS' " +
            "AND t.due_date = CURDATE() " +  // LEFT JOIN 조건으로 이동
            "WHERE m.member_id = :managerId " +
            "GROUP BY m.member_id, m.username", nativeQuery = true)
    List<Map<String, Object>> findTicketCountByManagerId(@Param("managerId") Long managerId);
}
