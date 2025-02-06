package com.quartz.checkin.event.listener;

import com.quartz.checkin.entity.AlertLog;
import com.quartz.checkin.event.CommentAddedEvent;
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
public class CommentAddedEventListener {

    private final WebhookService webhookService;
    private final AlertLogRepository alertLogRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentAddedEvent(CommentAddedEvent event) {
        try {
            Long parentId = event.getAgitId();
            String authorName = event.getComment().getMember().getUsername();
            String originalComment = event.getComment().getContent();

            String formattedComment = String.format("%s님이 댓글을 작성했습니다: \"%s\"", authorName, originalComment);

            log.info("댓글 추가 이벤트: Agit Id={}, 작성자={}, Comment={}", parentId, authorName, formattedComment);

            webhookService.addCommentToWebhookPost(parentId, formattedComment);

            AlertLog alertLog = AlertLog.builder()
                    .relatedId(event.getTicketId())
                    .relatedTable("comment")
                    .status("SUCCESS")
                    .type("COMMENT_ADDED")
                    .createdAt(LocalDateTime.now())
                    .build();

            alertLogRepository.save(alertLog);
            log.info("AlertLog 저장 완료: {}", formattedComment);

        } catch (Exception e) {
            log.error("티켓 댓글 추가 이벤트 처리 실패: {}", e.getMessage(), e);

            AlertLog alertLog = AlertLog.builder()
                    .relatedId(event.getTicketId())
                    .relatedTable("comment")
                    .status("FAILURE")
                    .type("COMMENT_ADDED")
                    .createdAt(LocalDateTime.now())
                    .build();

            alertLogRepository.save(alertLog);
        }
    }

}