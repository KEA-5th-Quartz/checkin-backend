package com.quartz.checkin.event.listener;

import com.quartz.checkin.entity.AlertLog;
import com.quartz.checkin.event.FileUploadedEvent;
import com.quartz.checkin.repository.AlertLogRepository;
import com.quartz.checkin.service.WebhookService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileUploadedEventListener {

    private final WebhookService webhookService;
    private final AlertLogRepository alertLogRepository;

    @Async
    @EventListener
    public void handleFileUploadedEvent(FileUploadedEvent event) {
        if (event.getAgitId() == null) {
            log.warn("아지트 게시글 ID가 없음 (첨부파일 업로드 알림 안됨): ticketId={}", event.getTicketId());
            return;
        }

        try {
            String formattedMessage = event.getUsername() + "님이 첨부파일을 업로드했습니다.";

            log.info("웹훅에 첨부파일 업로드 알림 요청: ticketId={}, agitId={}, message={}",
                    event.getTicketId(), event.getAgitId(), formattedMessage);

            webhookService.addCommentToWebhookPost(event.getAgitId(), formattedMessage);

            alertLogRepository.save(AlertLog.builder()
                    .createdAt(LocalDateTime.now())
                    .relatedId(event.getTicketId())
                    .relatedTable("ticket")
                    .status("SUCCESS")
                    .type("FILE_UPLOADED")
                    .build());

        } catch (Exception e) {
            log.error("웹훅 첨부파일 업로드 알림 실패: {}", e.getMessage());

            alertLogRepository.save(AlertLog.builder()
                    .createdAt(LocalDateTime.now())
                    .relatedId(event.getTicketId())
                    .relatedTable("ticket")
                    .status("FAILURE")
                    .type("FILE_UPLOADED")
                    .build());
        }
    }
}