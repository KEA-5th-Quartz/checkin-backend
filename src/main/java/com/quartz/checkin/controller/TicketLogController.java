package com.quartz.checkin.controller;

import com.quartz.checkin.dto.category.request.FirstCategoryPatchRequest;
import com.quartz.checkin.dto.category.request.SecondCategoryPatchRequest;
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
    @Operation(summary = "API 명세서 v0.3 line 46", description = "티켓 완료 처리")
    @PatchMapping("/{ticketId}/close")
    public ApiResponse<TicketLogResponse> closeTicket(
            @PathVariable("ticketId") Long ticketId,
            @AuthenticationPrincipal CustomUser user) {
        TicketLogResponse response = ticketLogService.closeTicket(user.getId(), ticketId);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Manager
    @Operation(summary = "API 명세서 v0.3 line 47", description = "1차 카테고리 수정")
    @PatchMapping("/{ticketId}/category")
    public ApiResponse<TicketLogResponse> updateFirstCategory(
            @PathVariable("ticketId") Long ticketId,
            @AuthenticationPrincipal CustomUser user,
            @RequestBody @Valid FirstCategoryPatchRequest request) {

        TicketLogResponse response = ticketLogService.updateFirstCategory(user.getId(), ticketId, request);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Manager
    @Operation(summary = "API 명세서 v0.3 line 48", description = "2차 카테고리 수정")
    @PatchMapping("/{ticketId}/category/{firstCategoryId}")
    public ApiResponse<TicketLogResponse> updateSecondCategory(
            @PathVariable("ticketId") Long ticketId,
            @PathVariable("firstCategoryId") Long firstCategoryId,
            @AuthenticationPrincipal CustomUser user,
            @RequestBody @Valid SecondCategoryPatchRequest request) {

        TicketLogResponse response = ticketLogService.updateSecondCategory(user.getId(), ticketId, firstCategoryId, request);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }
  
    @Manager
    @Operation(summary = "API 명세서 v0.3 line 49", description = "담당자 변경")
    @PatchMapping("/{ticketId}/assign")
    public ApiResponse<TicketLogResponse> reassignManager(
            @PathVariable("ticketId") Long ticketId,
            @AuthenticationPrincipal CustomUser user,
            @RequestBody Map<String, String> request) {

        TicketLogResponse response = ticketLogService.assignManager(user.getId(), ticketId, request.get("manager"));
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }
}
