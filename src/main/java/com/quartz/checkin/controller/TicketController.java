package com.quartz.checkin.controller;

import com.quartz.checkin.dto.request.TicketCommentRequest;
import com.quartz.checkin.dto.request.TicketCreateRequest;
import com.quartz.checkin.dto.response.ApiResponse;
import com.quartz.checkin.dto.response.CommentLikeListResponse;
import com.quartz.checkin.dto.response.CommentLikeResponse;
import com.quartz.checkin.dto.response.CommentResponse;
import com.quartz.checkin.dto.response.TicketActivityResponse;
import com.quartz.checkin.dto.response.TicketCreateResponse;
import com.quartz.checkin.service.TicketService;
import com.quartz.checkin.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final CommentService commentService;

    @PostMapping
    public ApiResponse<TicketCreateResponse> createTicket(
            @RequestHeader("memberId") Long memberId,
            @RequestBody @Valid TicketCreateRequest request) {
        TicketCreateResponse response = ticketService.createTicket(memberId, request);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @PostMapping("/{ticketId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> writeComment(
            @PathVariable Long ticketId,
            @Valid @RequestBody TicketCommentRequest request) {

        return ResponseEntity.ok(ApiResponse.createSuccessResponseWithData(200, commentService.writeComment(ticketId, request.getContent())));
    }

    @GetMapping("/{ticketId}/comments")
    public ResponseEntity<ApiResponse<TicketActivityResponse>> getCommentsAndLogs(
            @PathVariable Long ticketId) {

        return ResponseEntity.ok(ApiResponse.createSuccessResponseWithData(200, commentService.getCommentsAndLogs(ticketId)));
    }

    @PutMapping("/{ticketId}/comments/{commentId}/likes")
    public ResponseEntity<ApiResponse<CommentLikeResponse>> toggleLike(
            @PathVariable Long ticketId,
            @PathVariable Long commentId) {
        return ResponseEntity.ok(ApiResponse.createSuccessResponseWithData(200, commentService.toggleLike(ticketId, commentId)));
    }

    @GetMapping("/{ticketId}/comments/{commentId}/likes")
    public ResponseEntity<ApiResponse<CommentLikeListResponse>> getLikingMembersList(
            @PathVariable Long ticketId,
            @PathVariable Long commentId) {
        return ResponseEntity.ok(ApiResponse.createSuccessResponseWithData(200, commentService.getLikingMembersList(ticketId, commentId)));
    }

//    @PostMapping("/{ticketId}/comments/{commentId}/attachment")
//    public ResponseEntity<ApiResponse<CommentResponse>> uploadCommentAttachment(
//            @PathVariable Long ticketId,
//            @PathVariable Long commentId,
//            @RequestParam MultipartFile file) {
//        return ResponseEntity.ok(null);
//    }
}