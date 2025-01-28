package com.quartz.checkin.service;

import com.quartz.checkin.event.CommentAddedEvent;
import com.quartz.checkin.event.TicketAssigneeChangedEvent;
import com.quartz.checkin.event.TicketStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketEventListener {

    private final WebhookService webhookService;

    @Async
    @EventListener
    public void handleTicketStatusChangedEvent(TicketStatusChangedEvent event) {
        log.info("티켓 상태가 변경되었습니다: {}", event);

        Long userId = event.getUserId();
        webhookService.sendWebhook(event, "/ticket/status-changed", userId);
    }

    @Async
    @EventListener
    public void handleTicketAssigneeChangedEvent(TicketAssigneeChangedEvent event) {
        log.info("티켓 담당자가 변경되었습니다: {}", event);

        Long userId = event.getUserId();
        webhookService.sendWebhook(event, "/ticket/assignee-changed", userId);
    }

    @Async
    @EventListener
    public void handleCommentAddedEvent(CommentAddedEvent event) {
        log.info("티켓에 댓글이 달렸습니다: {}", event);

        Long receiverId;

        if (event.getCommenterId().equals(event.getUserId())) {
            receiverId = event.getManagerId();
        } else if (event.getCommenterId().equals(event.getManagerId())) {
            receiverId = event.getUserId();
        } else {
            log.warn("댓글 작성자 정보가 예상과 다릅니다: {}", event);
            return;
        }

        // 알림 전송
        webhookService.sendWebhook(event, "/ticket/comment-added", receiverId);
    }
}