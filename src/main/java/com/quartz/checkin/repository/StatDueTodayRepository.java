package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatDueTodayRepository extends JpaRepository<Member, Long> {

    @Query(value = """
        SELECT 
            m.username AS username,
            COUNT(t.ticket_id) AS ticketCount
        FROM 
            member m
        LEFT JOIN 
            ticket t ON m.member_id = t.manager_id
        WHERE 
            m.member_id = :managerId
            AND t.status = 'In Progress'
            AND t.due_date = CURDATE()
        GROUP BY 
            m.username
        """, nativeQuery = true)
    Optional<StatDueTodayRepository> findDueTodayTickets(@Param("managerId") Long managerId);
}
