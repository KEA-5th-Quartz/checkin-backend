package com.quartz.checkin.controller;

import com.quartz.checkin.common.exception.ApiResponse;
import com.quartz.checkin.dto.request.TicketCreateRequest;
import com.quartz.checkin.dto.response.TicketCreateResponse;
import com.quartz.checkin.service.TicketCrudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketCrudService ticketCRUDService;

    @PostMapping
    public ResponseEntity<ApiResponse<TicketCreateResponse>> createTicket(
            @RequestHeader("memberId") Long memberId,
            @RequestBody @Valid TicketCreateRequest request) {
        TicketCreateResponse response = ticketCRUDService.createTicket(memberId, request);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }
}

