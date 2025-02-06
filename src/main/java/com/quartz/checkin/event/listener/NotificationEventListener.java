package com.quartz.checkin.event.listener;

import com.quartz.checkin.dto.webhook.WebhookRequest;
import com.quartz.checkin.entity.AlertLog;
import com.quartz.checkin.event.NotificationEvent;
import com.quartz.checkin.repository.AlertLogRepository;
import com.quartz.checkin.service.WebhookService;
import java.util.ArrayList;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final WebhookService webhookService;
    private final AlertLogRepository alertLogRepository;

    @Async
    @EventListener
    public void handleNotificationEvent(NotificationEvent event) {
        if (("CATEGORY_CHANGED".equals(event.getType()) || "COMMENT_ADDED".equals(event.getType()))
                && event.getLogMessage() != null && event.getAgitId() != null) {

            Map<String, Object> payload = Map.of(
                    "parent_id", event.getAgitId(),
                    "text", event.getLogMessage()
            );

            webhookService.sendWebhook("/wall_messages/" + event.getAgitId() + "/comments", HttpMethod.POST, payload);
        }

        List<Long> receivers = determineReceivers(event);
        for (Long receiverId : receivers) {
            AlertLog alertLog = new AlertLog(
                    null,
                    receiverId,
                    event.getType(),
                    event.getRelatedTable(),
                    event.getRelatedId(),
                    "Open",
                    null
            );

            try {
                WebhookRequest webhookRequest = new WebhookRequest();
                webhookRequest.setText("새 알림이 생성되었습니다.");
                webhookRequest.setTask(new WebhookRequest.Task(event.getType(), List.of(receiverId.toString())));

                String fullUrl = "/wall_messages/" + event.getAgitId() + "/update_status";
                Map<String, Object> payload = Map.of(
                        "text", webhookRequest.getText(),
                        "task", webhookRequest.getTask()
                );

                webhookService.sendWebhook(fullUrl, HttpMethod.PUT, payload);
                alertLog.setStatus("SUCCESS");
            } catch (Exception e) {
                alertLog.setStatus("FAILURE");
                log.error("웹훅 호출 실패 (receiverId: {}): {}", receiverId, e.getMessage());
            } finally {
                alertLogRepository.save(alertLog);
            }
        }
    }

    private List<Long> determineReceivers(NotificationEvent event) {
        List<Long> receivers = new ArrayList<>();

        if ("COMMENT".equals(event.getType())) {
            if (event.getMemberId() != null) {
                // 1. 사용자가 댓글을 작성한 경우 → 담당자가 알림 받음
                if (event.getMemberId().equals(event.getUserId())) {
                    receivers.add(event.getManagerId());
                }
                //2. 담당자가 댓글을 작성한 경우 → 사용자(티켓 등록자)가 알림 받음
                else if (event.getMemberId().equals(event.getManagerId())) {
                    receivers.add(event.getUserId());
                }
                //3. 제3자가 댓글을 작성한 경우 → 사용자 + 담당자 모두 알림 받음
                else {
                    receivers.add(event.getUserId());
                    receivers.add(event.getManagerId());
                }
            }
        } else {
            // 댓글이 아닌 경우 → 티켓 등록자(사용자)만 알림 받음
            receivers.add(event.getUserId());
        }

        return receivers;
    }
}