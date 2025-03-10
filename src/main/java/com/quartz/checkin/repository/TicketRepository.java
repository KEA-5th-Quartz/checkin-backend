package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Category;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Ticket;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long>, TicketRepositoryCustom,
        TicketTrashRepositoryCustom {
    List<Ticket> findByUser(Member member);
    List<Ticket> findByManager(Member member);
    boolean existsByCustomId(String customId);
    boolean existsByFirstCategory(Category secondCategory);
    boolean existsBySecondCategory(Category secondCategory);
}
