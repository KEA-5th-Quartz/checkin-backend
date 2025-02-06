package com.quartz.checkin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.entity.Member;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MemberRepository memberRepository;

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
        String url = webhookUrl;
        if (action != null && !action.isEmpty()) {
            if (!url.endsWith("/")) {
                url += "/";
            }
            url += action;
        }

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

    public Long createAgitPost(String title, String content, List<String> assignees) {
        // URL 중복 제거
        String url = webhookUrl + "/wall_messages";

        Map<String, Object> payload = Map.of(
                "text", "**[티켓 생성] " + title + "**\n" + content,
                "task", Map.of(
                        "template_name", "web_bug",
                        "assignees", assignees
                )
        );

        try {
            log.info("웹훅 요청 URL: {}", url);
            log.info("웹훅 요청 payload: {}", payload);

            ResponseEntity<Map<String, Object>> response = sendWebhookRequest(url, payload, HttpMethod.POST);
            Map<String, Object> responseBody = response.getBody();

            log.info("웹훅 응답 데이터: {}", responseBody);

            if (responseBody != null && responseBody.containsKey("id")) {
                return Long.valueOf(responseBody.get("id").toString()); // ✅ 'agit_id' 대신 'id' 사용
            } else {
                log.warn("agit_id가 응답에 포함되지 않음. 게시물 ID 없이 진행.");
                return null;
            }
        } catch (Exception e) {
            log.error("아지트 게시물 생성 실패: {}", e.getMessage());
            throw new ApiException("웹훅 요청 실패: " + e.getMessage());
        }
    }

    @Async
    public void updateAssigneeInWebhook(Long agitId, Long managerId, Long userId) {
        if (agitId == null) {
            log.error("웹훅 담당자 변경 실패: agitId가 null입니다.");
            throw new IllegalArgumentException("agitId가 null일 수 없습니다.");
        }

        // 사용자 및 담당자의 username 조회
        String userUsername = memberRepository.findById(userId)
                .map(Member::getUsername)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));

        String managerUsername = memberRepository.findById(managerId)
                .map(Member::getUsername)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));

        // assignees 리스트 생성
        List<String> assignees = new ArrayList<>();
        assignees.add(userUsername);  // 사용자 추가
        assignees.add(managerUsername); // 담당자 추가

        // 웹훅 요청
        String url = webhookUrl + "/wall_messages/" + agitId + "/update_assignees";
        Map<String, Object> payload = Map.of("assignees", assignees);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("웹훅 담당자 변경 실패: " + response.getStatusCode());
            }

            log.info("웹훅 담당자 변경 성공: agitId={}, assignees={}", agitId, assignees);
        } catch (Exception e) {
            log.error("웹훅 담당자 변경 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("웹훅 담당자 변경 실패: " + e.getMessage());
        }
    }

    @Async
    public void updateStatusInWebhook(Long agitId, int status) {
        if (agitId == null) {
            log.error("웹훅 상태 변경 실패: agitId가 null입니다.");
            throw new IllegalArgumentException("agitId가 null일 수 없습니다.");
        }

        String url = webhookUrl + "/wall_messages/" + agitId + "/update_status";

        Map<String, Object> payload = Map.of("status", status);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("웹훅 상태 변경 실패: " + response.getStatusCode());
            }

            log.info("웹훅 상태 변경 성공: agitId={}, status={}", agitId, status);
        } catch (Exception e) {
            log.error("웹훅 상태 변경 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("웹훅 상태 변경 실패: " + e.getMessage());
        }
    }

    // 공통 웹훅 요청 처리 메서드
    public ResponseEntity<Map<String, Object>> sendWebhookRequest(String actionUrl, Map<String, Object> payload, HttpMethod method) {
        String url = webhookUrl;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            log.info("웹훅 요청 전송: {}", url);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, method, entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response == null || response.getBody() == null) {
                log.error("웹훅 요청 실패: 응답이 null입니다. (URL: {})", url);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "웹훅 응답 없음"));
            }

            log.info("웹훅 응답 데이터: {}", response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("웹훅 요청 실패: {} - {}", response.getStatusCode(), response.getBody());
                return ResponseEntity.status(response.getStatusCode()).body(Map.of("error", "웹훅 실패"));
            }

            return response;
        } catch (Exception e) {
            log.error("웹훅 전송 중 오류 발생 (URL: {}): {}", url, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "웹훅 오류: " + e.getMessage()));
        }
    }

    public void addCommentToWebhookPost(Long parentId, String commentText) {
        try {
            String url = webhookUrl + "/wall_messages/add_comment";

            Map<String, Object> payload = Map.of(
                    "parent_id", parentId,
                    "text", commentText
            );

            sendWebhookRequest(url, payload, HttpMethod.POST);
            log.info("웹훅 게시물({})에 댓글 추가: {}", parentId, commentText);
        } catch (Exception e) {
            log.error("웹훅 게시물({})에 댓글 추가 실패: {}", parentId, e.getMessage());
        }
    }

}