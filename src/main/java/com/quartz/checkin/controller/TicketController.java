package com.quartz.checkin.controller;

import com.quartz.checkin.config.S3Config;
import com.quartz.checkin.dto.common.response.ApiResponse;
import com.quartz.checkin.dto.common.response.UploadAttachmentsResponse;
import com.quartz.checkin.dto.ticket.request.PriorityUpdateRequest;
import com.quartz.checkin.dto.ticket.request.TicketCreateRequest;
import com.quartz.checkin.dto.ticket.request.TicketDeleteOrRestoreOrPurgeRequest;
import com.quartz.checkin.dto.ticket.request.TicketUpdateRequest;
import com.quartz.checkin.dto.ticket.response.AttachmentResponse;
import com.quartz.checkin.dto.ticket.response.ManagerTicketListResponse;
import com.quartz.checkin.dto.ticket.response.SoftDeletedTicketResponse;
import com.quartz.checkin.dto.ticket.response.TicketCreateResponse;
import com.quartz.checkin.dto.ticket.response.TicketDetailResponse;
import com.quartz.checkin.dto.ticket.response.TicketProgressResponse;
import com.quartz.checkin.dto.ticket.response.UserTicketListResponse;
import com.quartz.checkin.entity.Priority;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.security.CustomUser;
import com.quartz.checkin.security.annotation.Manager;
import com.quartz.checkin.security.annotation.ManagerOrUser;
import com.quartz.checkin.security.annotation.User;
import com.quartz.checkin.service.AttachmentService;
import com.quartz.checkin.service.TicketCudService;
import com.quartz.checkin.service.TicketQueryService;
import com.quartz.checkin.service.TicketTrashService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    private final AttachmentService attachmentService;
    private final TicketCudService ticketCudService;
    private final TicketQueryService ticketQueryService;
    private final TicketTrashService ticketTrashService;

    @User
    @Operation(summary = "API 명세서 v0.3 line 32", description = "티켓 생성")
    @PostMapping
    public ApiResponse<TicketCreateResponse> createTicket(
            @AuthenticationPrincipal CustomUser user,
            @RequestBody @Valid TicketCreateRequest request) {

        TicketCreateResponse response = ticketCudService.createTicket(user.getId(), request);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @User
    @Operation(summary = "API 명세서 v0.3 line 33", description = "티켓에 첨부파일 업로드")
    @PostMapping("/attachment")
    public ApiResponse<List<UploadAttachmentsResponse>> uploadAttachment(
            @RequestPart("files") List<MultipartFile> multipartFiles) {

        List<UploadAttachmentsResponse> response =
                attachmentService.uploadAttachments(multipartFiles, S3Config.TICKET_DIR);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @ManagerOrUser
    @Operation(summary = "API 명세서 v0.3 line 34", description = "티켓 첨부파일 다운로드")
    @GetMapping("/{ticketId}/{attachmentUrl}")
    public ApiResponse<AttachmentResponse> downloadAttachment(
            @PathVariable Long ticketId,
            @RequestParam String attachmentUrl
    ) {
        AttachmentResponse response =
                attachmentService.getAttachmentInfo(ticketId, attachmentUrl);

        return ApiResponse.createSuccessResponseWithData(
                HttpStatus.OK.value(),
                response
        );
    }


    @User
    @Operation(summary = "API 명세서 v0.3 line 35", description = "사용자가 티켓 수정")
    @PutMapping("/{ticketId}")
    public ApiResponse<Void> updateTicket(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal CustomUser user,
            @RequestBody @Valid TicketUpdateRequest request) {

        ticketCudService.updateTicket(user.getId(), request, ticketId);
        return ApiResponse.createSuccessResponse(HttpStatus.OK.value());
    }

    @PatchMapping
    @User
    @Operation(summary = "API 명세서 V0.3 line 36", description = "사용자가 다중 티켓 임시삭제")
    public ApiResponse<Void> tempDeleteTickets(
            @AuthenticationPrincipal CustomUser user,
            @RequestBody @Valid TicketDeleteOrRestoreOrPurgeRequest request) {

        ticketCudService.tempDeleteTickets(user.getId(), request.getTicketIds());

        return ApiResponse.createSuccessResponse(HttpStatus.OK.value());
    }


    @ManagerOrUser
    @Operation(summary = "API 명세서 v0.3 line 40", description = "티켓 상세 조회")
    @GetMapping("/{ticketId}")
    public ApiResponse<TicketDetailResponse> getTicketDetail(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal CustomUser user) {

        TicketDetailResponse response = ticketQueryService.getTicketDetail(user.getId(), ticketId);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Manager
    @Operation(summary = "API 명세서 v0.3 line 41", description = "담당자가 전체 티켓 조회")
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
            @RequestParam(defaultValue = "asc") String sortByCreatedAt,
            @AuthenticationPrincipal CustomUser user) {

        ManagerTicketListResponse response = ticketQueryService.getManagerTickets(
                user.getId(), statuses, usernames, categories, priorities, dueToday, dueThisWeek, page, size, sortByCreatedAt
        );

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Manager
    @Operation(summary = "API 명세서 v0.3 line 42", description = "담당자가 자신의 데이터 조회(내 티켓 현황)")
    @GetMapping("/progress")
    public ApiResponse<TicketProgressResponse> getManagerProgress(
            @AuthenticationPrincipal CustomUser user) {

        TicketProgressResponse response = ticketQueryService.getManagerProgress(user.getId());
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

@Manager
    @Operation(summary = "API 명세서 v0.3 line 44", description = "담당자 티켓 검색")
    @GetMapping("/search")
    public ApiResponse<ManagerTicketListResponse> searchTickets(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "asc") String sortByCreatedAt,
            @AuthenticationPrincipal CustomUser user) {

        ManagerTicketListResponse response = ticketQueryService.searchManagerTickets(user.getId(), keyword, page, size, sortByCreatedAt);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }


    @User
    @Operation(summary = "API 명세서 v0.3 line 43", description = "사용자 전체 티켓 조회")
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
            @RequestParam(defaultValue = "asc") String sortByCreatedAt,
            @AuthenticationPrincipal CustomUser user) {

        UserTicketListResponse response = ticketQueryService.getUserTickets(
                user.getId(), statuses, usernames, categories, priorities, dueToday, dueThisWeek, page, size, sortByCreatedAt
        );

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }


    @User
    @Operation(summary = "API 명세서 v0.3 line 45", description = "사용자 티켓 검색")
    @GetMapping("/my-tickets/search")
    public ApiResponse<UserTicketListResponse> searchUserTickets(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "asc") String sortByCreatedAt,
            @AuthenticationPrincipal CustomUser user) {

        UserTicketListResponse response = ticketQueryService.searchUserTickets(user.getId(), keyword, page, size, sortByCreatedAt);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Manager
    @Operation(summary = "API 명세서 v0.3 line 50", description = "중요도 변경")
    @PatchMapping("/{ticketId}/priority")
    public ApiResponse<Void> updateTicketPriority(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal CustomUser user,
            @RequestBody @Valid PriorityUpdateRequest request) {

        ticketCudService.updatePriority(user.getId(), ticketId, request);
        return ApiResponse.createSuccessResponse(HttpStatus.OK.value());
    }
  
    @User
    @Operation(summary = "API 명세서 v0.3 line 38", description = "사용자가 다중 티켓 복구")
    @PatchMapping("/trash/restore")
    public ApiResponse<Void> restoreTickets(
            @AuthenticationPrincipal CustomUser user,
            @RequestBody @Valid TicketDeleteOrRestoreOrPurgeRequest request) {

        ticketTrashService.restoreTickets(user.getId(), request.getTicketIds());
        return ApiResponse.createSuccessResponse(HttpStatus.OK.value());
    }

    @User
    @Operation(summary = "API 명세서 v0.3 line 39", description = "사용자가 다중 티켓 영구삭제")
    @DeleteMapping
    public ApiResponse<Void> deleteTickets(
            @AuthenticationPrincipal CustomUser user,
            @RequestBody @Valid TicketDeleteOrRestoreOrPurgeRequest request) {

        ticketTrashService.deleteTickets(user.getId(), request.getTicketIds());

        return ApiResponse.createSuccessResponse(HttpStatus.OK.value());
    }

    @User
    @Operation(summary = "API 명세서 v0.3 line 37", description = "휴지통 조회")
    @GetMapping("/trash")
    public ApiResponse<SoftDeletedTicketResponse> getDeletedTickets(
            @AuthenticationPrincipal CustomUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        SoftDeletedTicketResponse response = ticketTrashService.getDeletedTickets(user.getId(), page, size);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }
}

