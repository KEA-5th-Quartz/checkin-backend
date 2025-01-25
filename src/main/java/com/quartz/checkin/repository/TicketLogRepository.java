package com.quartz.checkin.repository;

import com.quartz.checkin.entity.TicketLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketLogRepository extends JpaRepository<TicketLog, Long> {
    List<TicketLog> findByTicketId(Long ticketId);
}
