package com.quartz.checkin.event.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quartz.checkin.entity.AlertLog;
import com.quartz.checkin.event.TicketAssigneeChangedEvent;
import com.quartz.checkin.repository.AlertLogRepository;
import com.quartz.checkin.service.MemberService;
import com.quartz.checkin.service.WebhookService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketAssigneeChangedEventListener {

    private final WebhookService webhookService;
    private final AlertLogRepository alertLogRepository;

    @Async
    @EventListener
    public void handleTicketAssigneeChangedEvent(TicketAssigneeChangedEvent event) {
        try {
            webhookService.updateAssigneeInWebhook(event.getAgitId(), event.getManagerId(), event.getAssigneeId());

            alertLogRepository.save(AlertLog.builder()
                    .createdAt(LocalDateTime.now())
                    .relatedId(event.getTicketId())
                    .relatedTable("ticket")
                    .status("SUCCESS")
                    .type("ASSIGNEE_CHANGED")
                    .build());

            log.info("담당자 변경 이벤트 처리 완료: AgitId={}, ManagerId={}, AssigneeId={}",
                    event.getAgitId(), event.getManagerId(), event.getAssigneeId());

        } catch (Exception e) {
            log.error("담당자 변경 이벤트 처리 실패 (AgitId={}): {}", event.getAgitId(), e.getMessage(), e);

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
