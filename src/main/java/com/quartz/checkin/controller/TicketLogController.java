package com.quartz.checkin.controller;

import com.quartz.checkin.dto.request.SecondCategoryUpdateRequest;
import com.quartz.checkin.dto.response.ApiResponse;
import com.quartz.checkin.dto.request.FirstCategoryUpdateRequest;
import com.quartz.checkin.dto.request.PriorityUpdateRequest;
import com.quartz.checkin.dto.response.TicketLogResponse;
import com.quartz.checkin.security.CustomUser;
import com.quartz.checkin.security.annotation.Manager;
import com.quartz.checkin.service.TicketLogService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketLogController {

    private final TicketLogService ticketLogService;

    @Manager
    @Operation(summary = "API 명세서 v0.1 line 35", description = "티켓 완료 처리")
    @PatchMapping("/{ticketId}/close")
    public ApiResponse<TicketLogResponse> closeTicket(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal CustomUser user) {
        TicketLogResponse response = ticketLogService.closeTicket(user.getId(), ticketId);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Manager
    @Operation(summary = "API 명세서 v0.1 line 36", description = "1차 카테고리 수정")
    @PatchMapping("/{ticketId}/category")
    public ApiResponse<TicketLogResponse> updateFirstCategory(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal CustomUser user,
            @RequestBody @Valid FirstCategoryUpdateRequest request) {

        TicketLogResponse response = ticketLogService.updateFirstCategory(user.getId(), ticketId, request);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Manager
    @Operation(summary = "API 명세서 v0.1 line 37", description = "2차 카테고리 수정")
    @PatchMapping("/{ticketId}/category/{firstCategoryId}")
    public ApiResponse<TicketLogResponse> updateSecondCategory(
            @PathVariable Long ticketId,
            @PathVariable Long firstCategoryId,
            @AuthenticationPrincipal CustomUser user,
            @RequestBody @Valid SecondCategoryUpdateRequest request) {

        TicketLogResponse response = ticketLogService.updateSecondCategory(user.getId(), ticketId, firstCategoryId, request);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }
  
    @Manager
    @Operation(summary = "API 명세서 v0.1 line 38", description = "담당자 변경")
    @PatchMapping("/{ticketId}/reassign")
    public ApiResponse<TicketLogResponse> reassignManager(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal CustomUser user,
            @RequestBody Map<String, String> request) {

        TicketLogResponse response = ticketLogService.reassignManager(user.getId(), ticketId, request.get("manager"));
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Manager
    @Operation(summary = "API 명세서 v0.1 line 39", description = "중요도 변경")
    @PatchMapping("/{ticketId}/priority")
    public ApiResponse<Void> updateTicketPriority(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal CustomUser user,
            @RequestBody @Valid PriorityUpdateRequest request) {

        ticketLogService.updatePriority(user.getId(), ticketId, request);
        return ApiResponse.createSuccessResponse(HttpStatus.OK.value());
    }

    @Manager
    @Operation(summary = "API 명세서 v0.1 line 40", description = "담당자 할당")
    @PatchMapping("/{ticketId}/assign")
    public ApiResponse<TicketLogResponse> assignManager(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal CustomUser user
    ) {
        TicketLogResponse response = ticketLogService.assignManager(user.getId(), ticketId);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }
}
