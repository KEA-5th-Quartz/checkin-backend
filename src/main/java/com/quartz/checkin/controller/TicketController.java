package com.quartz.checkin.controller;

import com.quartz.checkin.dto.request.TicketCreateRequest;
import com.quartz.checkin.dto.response.ApiResponse;
import com.quartz.checkin.dto.response.TicketCreateResponse;
import com.quartz.checkin.dto.response.TicketDetailResponse;
import com.quartz.checkin.security.CustomUser;
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
}

