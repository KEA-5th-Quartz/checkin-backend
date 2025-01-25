package com.quartz.checkin.controller;

import com.quartz.checkin.common.exception.ApiResponse;
import com.quartz.checkin.dto.response.TicketLogResponse;
import com.quartz.checkin.service.TicketLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketLogController {

    private final TicketLogService ticketLogService;

    @PatchMapping("/{ticketId}/assign")
    public ResponseEntity<ApiResponse<TicketLogResponse>> assignManager(
            @RequestHeader("memberId") Long memberId, // 헤더에서 담당자 ID 받기
            @PathVariable Long ticketId
    ) {
        TicketLogResponse response = ticketLogService.assignManager(memberId, ticketId);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    @PatchMapping("/{ticketId}/close")
    public ResponseEntity<ApiResponse<TicketLogResponse>> closeTicket(
            @RequestHeader("memberId") Long memberId,
            @PathVariable Long ticketId) {
        TicketLogResponse response = ticketLogService.closeTicket(memberId, ticketId);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}
