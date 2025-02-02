package com.quartz.checkin.controller;

import com.quartz.checkin.dto.category.request.FirstCategoryUpdateRequest;
import com.quartz.checkin.dto.category.request.SecondCategoryUpdateRequest;
import com.quartz.checkin.dto.common.response.ApiResponse;
import com.quartz.checkin.dto.ticket.response.TicketLogResponse;
import com.quartz.checkin.security.CustomUser;
import com.quartz.checkin.security.annotation.Manager;
import com.quartz.checkin.service.TicketLogService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketLogController {

    private final TicketLogService ticketLogService;

    @Manager
    @Operation(summary = "API 명세서 v0.2 line 43", description = "티켓 완료 처리")
    @PatchMapping("/{ticketId}/close")
    public ApiResponse<TicketLogResponse> closeTicket(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal CustomUser user) {
        TicketLogResponse response = ticketLogService.closeTicket(user.getId(), ticketId);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Manager
    @Operation(summary = "API 명세서 v0.2 line 44", description = "1차 카테고리 수정")
    @PatchMapping("/{ticketId}/category")
    public ApiResponse<TicketLogResponse> updateFirstCategory(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal CustomUser user,
            @RequestBody @Valid FirstCategoryUpdateRequest request) {

        TicketLogResponse response = ticketLogService.updateFirstCategory(user.getId(), ticketId, request);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Manager
    @Operation(summary = "API 명세서 v0.2 line 45", description = "2차 카테고리 수정")
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
    @Operation(summary = "API 명세서 v0.2 line 46", description = "담당자 변경")
    @PatchMapping("/{ticketId}/reassign")
    public ApiResponse<TicketLogResponse> reassignManager(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal CustomUser user,
            @RequestBody Map<String, String> request) {

        TicketLogResponse response = ticketLogService.reassignManager(user.getId(), ticketId, request.get("manager"));
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Manager
    @Operation(summary = "API 명세서 v0.2 line 42", description = "담당자 할당")
    @PatchMapping("/{ticketId}/assign")
    public ApiResponse<TicketLogResponse> assignManager(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal CustomUser user
    ) {
        TicketLogResponse response = ticketLogService.assignManager(user.getId(), ticketId);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }
}
