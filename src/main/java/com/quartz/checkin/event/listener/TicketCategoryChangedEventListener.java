package com.quartz.checkin.event.listener;

import com.quartz.checkin.event.TicketCategoryChangedEvent;
import com.quartz.checkin.service.WebhookService;
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
class TicketCategoryChangedEventListener {

    private final WebhookService webhookService;

    @Async
    @EventListener
    public void handleTicketCategoryChangedEvent(TicketCategoryChangedEvent event) {
        String logMessage = String.format("카테고리가 변경되었습니다: '%s' → '%s'", event.getOldCategory(), event.getNewCategory());

        Map<String, Object> payload = Map.of(
                "parent_id", event.getTicketId(),
                "text", logMessage
        );

        webhookService.sendWebhookRequest("/wall_messages/" + event.getTicketId() + "/comments", payload, HttpMethod.POST);
        log.info("Ticket Category Changed Event Processed: TicketId={}", event.getTicketId());
    }
}

