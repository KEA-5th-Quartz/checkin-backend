package com.quartz.checkin.event.listener;

import com.quartz.checkin.entity.AlertLog;
import com.quartz.checkin.event.TicketCreatedEvent;
import com.quartz.checkin.repository.AlertLogRepository;
import com.quartz.checkin.service.WebhookService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketCreatedEventListener {

    private final AlertLogRepository alertLogRepository;
    private final WebhookService webhookService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTicketCreatedEvent(TicketCreatedEvent event) {
        try {
            Long agitId = webhookService.createAgitPost(
                    event.getId(),
                    event.getTitle(),
                    event.getContent(),
                    event.getAssignees()
            );

            webhookService.updateAgitIdInTicket(event.getId(), agitId);

            AlertLog alertLog = AlertLog.builder()
                    .createdAt(LocalDateTime.now())
                    .memberId(event.getUserId())
                    .relatedId(event.getId())
                    .relatedTable("ticket")
                    .status("SUCCESS")
                    .type("TICKET_CREATED")
                    .build();
            alertLogRepository.save(alertLog);

            log.info("Ticket Created Event Processed: TicketId={}", event.getId());
        } catch (Exception e) {
            log.error("Failed to process TicketCreatedEvent: {}", e.getMessage());

            AlertLog alertLog = AlertLog.builder()
                    .createdAt(LocalDateTime.now())
                    .memberId(event.getUserId())
                    .relatedId(event.getId())
                    .relatedTable("ticket")
                    .status("FAILURE")
                    .type("TICKET_CREATED")
                    .build();
            alertLogRepository.save(alertLog);
        }
    }
}

