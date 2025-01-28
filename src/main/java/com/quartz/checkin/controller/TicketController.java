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

import java.util.List;

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
            @RequestParam(required = false) List<Status> statuses,
            @RequestParam(required = false) List<String> usernames,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<Priority> priorities,
            @RequestParam(required = false) Boolean dueToday,
            @RequestParam(required = false) Boolean dueThisWeek,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUser user) {

        ManagerTicketListResponse response = ticketCrudService.getManagerTickets(
                user.getId(), statuses, usernames, categories, priorities, dueToday, dueThisWeek, page, size
        );

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Manager
    @Operation(summary = "API 명세서 v0.1 line 33", description = "담당자 티켓 검색")
    @GetMapping("/search")
    public ApiResponse<ManagerTicketListResponse> searchTickets(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUser user) {

        ManagerTicketListResponse response = ticketCrudService.searchManagerTickets(user.getId(), keyword, page, size);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }


    @User
    @Operation(summary = "API 명세서 v0.1 line 32", description = "사용자 전체 티켓 조회")
    @GetMapping("/my-tickets")
    public ApiResponse<UserTicketListResponse> getUserTickets(
            @RequestParam(required = false) List<Status> statuses,
            @RequestParam(required = false) List<String> usernames,
            @RequestParam(required = false) List<String> categories,
            @RequestParam(required = false) List<Priority> priorities,
            @RequestParam(required = false) Boolean dueToday,
            @RequestParam(required = false) Boolean dueThisWeek,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUser user) {

        UserTicketListResponse response = ticketCrudService.getUserTickets(
                user.getId(), statuses, usernames, categories, priorities, dueToday, dueThisWeek, page, size
        );

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }


    @User
    @Operation(summary = "API 명세서 v0.1 line 34", description = "사용자 티켓 검색")
    @GetMapping("/my-tickets/search")
    public ApiResponse<UserTicketListResponse> searchUserTickets(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUser user) {

        UserTicketListResponse response = ticketCrudService.searchUserTickets(user.getId(), keyword, page, size);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }
}

