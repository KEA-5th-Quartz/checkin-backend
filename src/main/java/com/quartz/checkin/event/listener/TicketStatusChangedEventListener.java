package com.quartz.checkin.event.listener;

import com.quartz.checkin.entity.AlertLog;
import com.quartz.checkin.event.TicketStatusChangedEvent;
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
public class TicketStatusChangedEventListener {

    private final WebhookService webhookService;
    private final AlertLogRepository alertLogRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTicketStatusChangedEvent(TicketStatusChangedEvent event) {
        try {
            webhookService.updateStatusInWebhook(event.getAgitId(), event.getStatus());

            alertLogRepository.save(AlertLog.builder()
                    .createdAt(LocalDateTime.now())
                    .relatedId(event.getTicketId())
                    .relatedTable("ticket")
                    .status("SUCCESS")
                    .type("STATUS_CHANGED")
                    .build());

            log.info("Ticket Status Changed Event 처리 완료: TicketId={}, NewStatus={}",
                    event.getTicketId(), event.getStatus());

        } catch (Exception e) {
            log.error("상태 변경 이벤트 처리 실패 (AgitId={}): {}", event.getAgitId(), e.getMessage(), e);

            alertLogRepository.save(AlertLog.builder()
                    .createdAt(LocalDateTime.now())
                    .relatedId(event.getTicketId())
                    .relatedTable("ticket")
                    .status("FAILURE")
                    .type("STATUS_CHANGED")
                    .build());
        }
    }
}