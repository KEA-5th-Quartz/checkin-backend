package com.quartz.checkin.controller;

import com.quartz.checkin.dto.request.WebhookAssigneesUpdate;
import com.quartz.checkin.dto.request.WebhookRequest;
import com.quartz.checkin.dto.request.WebhookStatusUpdate;
import com.quartz.checkin.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    // 요청 등록
    @PostMapping("/register")
    public ResponseEntity<String> registerRequest(@RequestBody WebhookRequest request) {
        webhookService.registerRequest(request);
        return ResponseEntity.ok("요청 등록 성공!");
    }

    // 상태 변경
    @PutMapping("/{id}/update-status")
    public ResponseEntity<String> updateStatus(@PathVariable String id, @RequestBody WebhookStatusUpdate statusUpdate) {
        webhookService.updateStatus(id, statusUpdate);
        return ResponseEntity.ok("상태 변경 성공!");
    }

    // 담당자 변경
    @PutMapping("/{id}/update-assignees")
    public ResponseEntity<String> updateAssignees(@PathVariable String id, @RequestBody WebhookAssigneesUpdate assigneesUpdate) {
        webhookService.updateAssignees(id, assigneesUpdate);
        return ResponseEntity.ok("담당자 등록 성공!");
    }
}

