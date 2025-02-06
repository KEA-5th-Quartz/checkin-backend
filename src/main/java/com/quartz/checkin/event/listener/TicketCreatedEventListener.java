package com.quartz.checkin.event.listener;

import com.quartz.checkin.entity.AlertLog;
import com.quartz.checkin.event.TicketCreatedEvent;
import com.quartz.checkin.repository.AlertLogRepository;
import java.time.LocalDateTime;
import java.util.Map;
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

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTicketCreatedEvent(TicketCreatedEvent event) {
        try {
            Map<String, Object> payload = Map.of(
                    "text", "**[티켓 생성] " + event.getTitle() + "**",
                    "task", Map.of("template_name", "티켓 생성")
            );

            AlertLog alertLog = AlertLog.builder()
                    .createdAt(LocalDateTime.now())
                    .memberId(event.getUserId())
                    .relatedId(event.getTicketId())
                    .relatedTable("ticket")
                    .status("SUCCESS")
                    .type("TICKET_CREATED")
                    .build();
            alertLogRepository.save(alertLog);

            log.info("Ticket Created Event Processed: TicketId={}", event.getTicketId());
        } catch (Exception e) {
            log.error("Failed to process TicketCreatedEvent: {}", e.getMessage());

            AlertLog alertLog = AlertLog.builder()
                    .createdAt(LocalDateTime.now())
                    .memberId(event.getUserId())
                    .relatedId(event.getTicketId())
                    .relatedTable("ticket")
                    .status("FAILURE")
                    .type("TICKET_CREATED")
                    .build();
            alertLogRepository.save(alertLog);
        }
    }
}

