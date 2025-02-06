package com.quartz.checkin.event.listener;

import com.quartz.checkin.entity.AlertLog;
import com.quartz.checkin.event.TicketCategoryChangedEvent;
import com.quartz.checkin.repository.AlertLogRepository;
import com.quartz.checkin.service.WebhookService;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketCategoryChangedEventListener {

    private final WebhookService webhookService;
    private final AlertLogRepository alertLogRepository;

    @Async
    @EventListener
    public void handleTicketCategoryChangedEvent(TicketCategoryChangedEvent event) {
        try {
            log.info("Processing TicketCategoryChangedEvent: AgitId={}, LogMessage={}", event.getAgitId(), event.getLogMessage());

            webhookService.addCommentToWebhookPost(event.getAgitId(), event.getLogMessage());

            alertLogRepository.save(AlertLog.builder()
                    .createdAt(LocalDateTime.now())
                    .relatedId(event.getTicketId())
                    .relatedTable("ticket")
                    .status("SUCCESS")
                    .type("STATUS_CHANGED")
                    .build());

        } catch (Exception e) {
            log.error("Failed to process TicketCategoryChangedEvent: {}", e.getMessage());

            alertLogRepository.save(AlertLog.builder()
                    .createdAt(LocalDateTime.now())
                    .relatedId(event.getTicketId())
                    .relatedTable("ticket")
                    .status("FAILURE")
                    .type("ASSIGNEE_CHANGED")
                    .build());
        }
    }
}
