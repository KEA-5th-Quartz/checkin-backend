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
        log.info("티켓 삭제 이벤트 감지: agitIds={}, ticketId={}", event.getAgitIds(), event.getTicketId());

        try {
            for (Long agitId : event.getAgitIds()) {
                log.info("아지트 게시글 삭제 요청: agitId={}", agitId);

                boolean success = webhookService.deleteAgitPost(agitId);

                if (success) {
                    log.info("아지트 게시글 삭제 성공: agitId={}", agitId);

                    alertLogRepository.save(AlertLog.builder()
                            .createdAt(LocalDateTime.now())
                            .relatedId(event.getTicketId())
                            .relatedTable("ticket")
                            .status("SUCCESS")
                            .type("TICKET_DELETED")
                            .build());

                } else {
                    log.error("아지트 게시글 삭제 실패(응답 오류): agitId={}", agitId);
                }
            }
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
