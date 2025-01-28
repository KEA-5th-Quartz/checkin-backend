package com.quartz.checkin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${webhook.url}")
    private String webhookUrl;

    /**
     * Webhook 요청 전송
     *
     * @param request    전송할 데이터 (이벤트 객체)
     * @param action     Webhook 엔드포인트
     * @param receiverId 수신자 ID
     */
    @Async
    public void sendWebhook(Object request, String action, Long receiverId) {
        String url = webhookUrl + action;

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("data", request);
            payload.put("receiverId", receiverId);

            String payloadJson = objectMapper.writeValueAsString(payload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(payloadJson, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to send webhook. Status: " + response.getStatusCode());
            }

            log.info("Webhook sent successfully to {} for receiverId: {}", url, receiverId);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize webhook request: {}", e.getMessage());
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("Error while sending webhook: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Async
    public void updateAssigneeInWebhook(Long messageId, Long newManagerId) {
        try {
            String url = webhookUrl + "/wall_messages/" + messageId + "/update_assignees";
            List<String> assignees = new ArrayList<>();

            // 새로운 담당자 추가
            if (newManagerId != null) {
                assignees.add(newManagerId.toString());
            }

            // Webhook 요청 데이터 생성
            Map<String, Object> payload = Map.of("assignees", assignees);

            sendWebhookRequest(url, payload, HttpMethod.PUT);
            log.info("담당자 변경 Webhook 요청 완료 (messageId: {}, assignees: {})", messageId, assignees);

        } catch (Exception e) {
            log.error("담당자 변경 Webhook 요청 실패 (messageId: {}): {}", messageId, e.getMessage());
        }
    }

    // 공통 웹훅 요청 처리 메서드
    private void sendWebhookRequest(String url, Map<String, Object> payload, HttpMethod method) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
            restTemplate.exchange(url, method, entity, String.class);
        } catch (Exception e) {
            log.error("웹훅 요청 실패 ({}): {}", url, e.getMessage());
        }
    }

}