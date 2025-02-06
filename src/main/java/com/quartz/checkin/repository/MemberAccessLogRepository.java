package com.quartz.checkin.repository;

import com.quartz.checkin.entity.MemberAccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MemberAccessLogRepository extends JpaRepository<MemberAccessLog, Long> {

    @Query(value = "SELECT al FROM MemberAccessLog al JOIN FETCH al.member",
            countQuery = "SELECT COUNT(al) FROM MemberAccessLog al")
    Page<MemberAccessLog> findAllJoinFetch(Pageable pageable);
}
