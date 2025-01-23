package com.quartz.checkin.controller;

import com.quartz.checkin.common.exception.ApiResponse;
import com.quartz.checkin.dto.request.TicketRequest;
import com.quartz.checkin.dto.response.TicketResponse;
import com.quartz.checkin.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(
            @RequestHeader("memberId") Long memberId,
            @RequestBody @Valid TicketRequest request) {
        TicketResponse response = ticketService.createTicket(memberId, request);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}

