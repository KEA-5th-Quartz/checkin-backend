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

    @Query("SELECT DISTINCT ta.attachment.id FROM TicketAttachment ta WHERE ta.attachment.id IN :attachmentIds")
    List<Long> findAttachmentIdsInUse(@Param("attachmentIds") List<Long> attachmentIds);

    @Query("SELECT ta.attachment.id FROM TicketAttachment ta WHERE ta.ticket.id IN :ticketIds")
    List<Long> findAttachmentIdsByTicketIds(@Param("ticketIds") List<Long> ticketIds);

    @Modifying
    @Query("DELETE FROM TicketAttachment ta WHERE ta.ticket.id IN :ticketIds")
    void deleteAllByTicketIds(@Param("ticketIds") List<Long> ticketIds);
}
