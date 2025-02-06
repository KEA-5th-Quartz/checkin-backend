package com.quartz.checkin.event.listener;

import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.event.AgitPostCreateEvent;
import com.quartz.checkin.repository.TicketRepository;
import com.quartz.checkin.service.WebhookService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgitPostCreateEventListener {

    private final WebhookService webhookService;
    private final TicketRepository ticketRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAgitPostCreateEvent(AgitPostCreateEvent event) {
        try {
            Long agitId = webhookService.createAgitPost(
                    event.getTitle(),
                    event.getContent(),
                    List.of(event.getUsername())
            );

            Ticket ticket = ticketRepository.findById(event.getTicketId())
                    .orElseThrow(() -> new RuntimeException("Ticket not found!"));

            ticket.linkToAgit(agitId);
            ticketRepository.save(ticket);

            log.info("AgitId updated for TicketId={} -> agitId={}", event.getTicketId(), agitId);
        } catch (Exception e) {
            log.error("Failed to create Agit Post for TicketId={}: {}", event.getTicketId(), e.getMessage());
        }
    }
}

