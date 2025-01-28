package com.quartz.checkin.controller;

import com.quartz.checkin.dto.response.ApiResponse;
import com.quartz.checkin.dto.request.CategoryUpdateRequest;
import com.quartz.checkin.dto.request.PriorityUpdateRequest;
import com.quartz.checkin.dto.response.TicketLogResponse;
import com.quartz.checkin.service.TicketLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketLogController {

    private final TicketLogService ticketLogService;

    @PatchMapping("/{ticketId}/assign")
    public ApiResponse<TicketLogResponse> assignManager(
            @RequestHeader("memberId") Long memberId, // 헤더에서 담당자 ID 받기
            @PathVariable Long ticketId
    ) {
        TicketLogResponse response = ticketLogService.assignManager(memberId, ticketId);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @PatchMapping("/{ticketId}/close")
    public ApiResponse<TicketLogResponse> closeTicket(
            @RequestHeader("memberId") Long memberId,
            @PathVariable Long ticketId) {
        TicketLogResponse response = ticketLogService.closeTicket(memberId, ticketId);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @PatchMapping("/{ticketId}/category")
    public ApiResponse<TicketLogResponse> updateFirstCategory(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal CustomUser user,
            @RequestBody @Valid FirstCategoryUpdateRequest request) {

        TicketLogResponse response = ticketLogService.updateFirstCategory(user.getId(), ticketId, request);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @PatchMapping("/{ticketId}/reassign")
    public ApiResponse<TicketLogResponse> reassignManager(
            @RequestHeader("memberId") Long memberId,
            @PathVariable Long ticketId,
            @RequestBody Map<String, String> request) {

        TicketLogResponse response = ticketLogService.reassignManager(memberId, ticketId, request.get("manager"));
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @PatchMapping("/{ticketId}/priority")
    public ApiResponse<TicketLogResponse> updateTicketPriority(
            @RequestHeader("memberId") Long memberId,
            @PathVariable Long ticketId,
            @RequestBody @Valid PriorityUpdateRequest request) {

        ticketLogService.updatePriority(memberId, ticketId, request);
        return ApiResponse.createSuccessResponse(HttpStatus.OK.value());
    }

}
