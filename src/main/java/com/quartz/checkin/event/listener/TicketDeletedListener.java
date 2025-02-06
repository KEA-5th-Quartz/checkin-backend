package com.quartz.checkin.event.listener;

import com.quartz.checkin.entity.AlertLog;
import com.quartz.checkin.event.TicketDeletedEvent;
import com.quartz.checkin.repository.AlertLogRepository;
import com.quartz.checkin.service.WebhookService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketDeletedListener {

    private final WebhookService webhookService;
    private final AlertLogRepository alertLogRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTicketDeletedEvent(TicketDeletedEvent event) {
        log.info("티켓 삭제 이벤트 감지: agitId={}, ticketId={}", event.getAgitIds(), event.getTicketId());

        try {
            webhookService.deleteAgitPost(event.getAgitIds());

            alertLogRepository.save(AlertLog.builder()
                    .createdAt(LocalDateTime.now())
                    .relatedId(event.getTicketId())
                    .relatedTable("ticket")
                    .status("SUCCESS")
                    .type("TICKET_DELETED")
                    .build());

            log.info("아지트 게시글 삭제 성공: ticketId={}", event.getTicketId());

        } catch (Exception e) {
            log.error("아지트 게시글 삭제 실패: {}", e.getMessage());

            alertLogRepository.save(AlertLog.builder()
                    .createdAt(LocalDateTime.now())
                    .relatedId(event.getTicketId())
                    .relatedTable("ticket")
                    .status("FAILURE")
                    .type("TICKET_DELETED")
                    .build());
        }
    }
}
