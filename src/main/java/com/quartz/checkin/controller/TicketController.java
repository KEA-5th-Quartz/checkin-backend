package com.quartz.checkin.controller;

import com.quartz.checkin.dto.common.response.ApiResponse;
import com.quartz.checkin.dto.ticket.request.PriorityUpdateRequest;
import com.quartz.checkin.dto.ticket.request.TicketCreateRequest;
import com.quartz.checkin.dto.ticket.response.ManagerTicketListResponse;
import com.quartz.checkin.dto.ticket.response.TicketAttachmentResponse;
import com.quartz.checkin.dto.ticket.response.TicketCreateResponse;
import com.quartz.checkin.dto.ticket.response.TicketDetailResponse;
import com.quartz.checkin.dto.ticket.response.UserTicketListResponse;
import com.quartz.checkin.entity.Priority;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.security.CustomUser;
import com.quartz.checkin.security.annotation.Manager;
import com.quartz.checkin.security.annotation.ManagerOrUser;
import com.quartz.checkin.security.annotation.User;
import com.quartz.checkin.service.TicketCudService;
import com.quartz.checkin.service.TicketQueryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketCudService ticketCudService;
    private final TicketQueryService ticketQueryService;

    @User
    @Operation(summary = "API 명세서 v0.1 line 25", description = "티켓 생성")
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ApiResponse<TicketCreateResponse> createTicket(
            @AuthenticationPrincipal CustomUser user,
            @RequestPart("request") @Valid TicketCreateRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws IOException {

        TicketCreateResponse response = ticketCudService.createTicket(user.getId(), request, files);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }
    @User
    @Operation(summary = "API 명세서 v0.1 line 41", description = "티켓에 첨부파일 업로드")
    @PostMapping("/{ticketId}/attachment")
    public ApiResponse<TicketAttachmentResponse> uploadAttachment(
            @PathVariable Long ticketId,
            @RequestParam("file") MultipartFile file) throws IOException {

        TicketAttachmentResponse response = ticketCudService.uploadAttachment(ticketId, file);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @ManagerOrUser
    @Operation(summary = "API 명세서 v0.1 line 29", description = "티켓 상세 조회")
    @GetMapping("/{ticketId}")
    public ApiResponse<TicketDetailResponse> getTicketDetail(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal CustomUser user) {

        TicketDetailResponse response = ticketQueryService.getTicketDetail(user.getId(),ticketId);
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
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUser user) {

        ManagerTicketListResponse response = ticketQueryService.getManagerTickets(
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
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUser user) {

        ManagerTicketListResponse response = ticketQueryService.searchManagerTickets(user.getId(), keyword, page, size);
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
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUser user) {

        UserTicketListResponse response = ticketQueryService.getUserTickets(
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
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUser user) {

        UserTicketListResponse response = ticketQueryService.searchUserTickets(user.getId(), keyword, page, size);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Manager
    @Operation(summary = "API 명세서 v0.1 line 38", description = "중요도 변경")
    @PatchMapping("/{ticketId}/priority")
    public ApiResponse<Void> updateTicketPriority(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal CustomUser user,
            @RequestBody @Valid PriorityUpdateRequest request) {

        ticketCudService.updatePriority(user.getId(), ticketId, request);
        return ApiResponse.createSuccessResponse(HttpStatus.OK.value());
    }
}

