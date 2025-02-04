package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.entity.TicketAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {
    List<TicketAttachment> findByTicketId(Long ticketId);

    @Modifying
    @Query("DELETE FROM TicketAttachment ta WHERE ta.ticket = :ticket AND ta.attachment.id IN :attachmentIds")
    void deleteByTicketAndAttachmentIds(@Param("ticket") Ticket ticket, @Param("attachmentIds") List<Long> attachmentIds);
}
