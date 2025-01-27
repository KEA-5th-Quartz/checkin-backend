package com.quartz.checkin.controller;

import com.quartz.checkin.dto.request.TicketCreateRequest;
import com.quartz.checkin.dto.response.ApiResponse;
import com.quartz.checkin.dto.response.TicketCreateResponse;
import com.quartz.checkin.service.TicketCrudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketCrudService ticketCrudService;

    @PostMapping
    public ApiResponse<TicketCreateResponse> createTicket(
            @RequestHeader("memberId") Long memberId,
            @RequestBody @Valid TicketCreateRequest request) {
        TicketCreateResponse response = ticketCrudService.createTicket(memberId, request);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }
}

