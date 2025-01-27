package com.quartz.checkin.controller;

import com.quartz.checkin.dto.request.TicketCreateRequest;
import com.quartz.checkin.dto.response.*;
import com.quartz.checkin.entity.Priority;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.security.CustomUser;
import com.quartz.checkin.security.annotation.Manager;
import com.quartz.checkin.security.annotation.ManagerOrUser;
import com.quartz.checkin.security.annotation.User;
import com.quartz.checkin.service.TicketCrudService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketCrudService ticketCrudService;

    @User
    @PostMapping
    public ApiResponse<TicketCreateResponse> createTicket(
            @AuthenticationPrincipal CustomUser user,
            @RequestBody @Valid TicketCreateRequest request) {
        TicketCreateResponse response = ticketCrudService.createTicket(user.getId(), request);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @ManagerOrUser
    @Operation(summary = "API 명세서 v0.1 line 29", description = "티켓 상세 조회")
    @GetMapping("/{ticketId}")
    public ApiResponse<TicketDetailResponse> getTicketDetail(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal CustomUser user) {

        TicketDetailResponse response = ticketCrudService.getTicketDetail(user.getId(),ticketId);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Manager
    @Operation(summary = "API 명세서 v0.1 line 30", description = "담당자 전체 티켓 조회")
    @GetMapping
    public ApiResponse<ManagerTicketListResponse> getTickets(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Priority priority,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUser user) {

        ManagerTicketListResponse response = ticketCrudService.getManagerTickets(user.getId(), status, username, category, priority, page, size);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @User
    @GetMapping("/my-tickets")
    public ApiResponse<UserTicketListResponse> getUserTickets(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUser user) {

        UserTicketListResponse response = ticketCrudService.getUserTickets(user.getId(), status, username, category, page, size);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }
}

