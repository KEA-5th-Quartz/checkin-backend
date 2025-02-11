package com.quartz.checkin.controller;

import com.quartz.checkin.dto.comment.response.CommentAttachmentResponse;
import com.quartz.checkin.dto.comment.response.CommentLikeListResponse;
import com.quartz.checkin.dto.comment.response.CommentLikeResponse;
import com.quartz.checkin.dto.comment.response.CommentResponse;
import com.quartz.checkin.dto.common.response.ApiResponse;
import com.quartz.checkin.dto.ticket.request.TicketCommentRequest;
import com.quartz.checkin.dto.ticket.response.TicketActivityResponse;
import com.quartz.checkin.security.CustomUser;
import com.quartz.checkin.security.annotation.ManagerOrUser;
import com.quartz.checkin.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @ManagerOrUser
    @Operation(summary = "API 명세서 v0.3 line 52", description = "티켓에 텍스트 타입 댓글 작성")
    @PostMapping("/{ticketId}/comments")
    public ApiResponse<CommentResponse> writeComment(
            @AuthenticationPrincipal CustomUser customUser,
            @Parameter(description = "티켓 ID", required = true) @PathVariable("ticketId") Long ticketId,
            @Valid @RequestBody TicketCommentRequest request) {
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), commentService.writeComment(customUser, ticketId,
                request.getContent()));
    }

    @Operation(summary = "API 명세서 v0.3 line 54", description = "티켓의 로그와 댓글 전체 조회")
    @GetMapping("/{ticketId}/comments")
    public ApiResponse<TicketActivityResponse> getCommentsAndLogs(
            @Parameter(description = "티켓 ID", required = true) @PathVariable("ticketId") Long ticketId) {
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), commentService.getCommentsAndLogs(ticketId));
    }

    @ManagerOrUser
    @Operation(summary = "API 명세서 v0.3 line 55", description = "특정 댓글에 좋아요 토글")
    @PutMapping("/{ticketId}/comments/{commentId}/likes")
    public ApiResponse<CommentLikeResponse> toggleLike(
            @AuthenticationPrincipal CustomUser user,
            @PathVariable("ticketId") Long ticketId,
            @PathVariable("commentId") Long commentId) {

        CommentLikeResponse response = commentService.toggleLike(user, ticketId, commentId);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Operation(summary = "API 명세서 v0.3 line 56", description = "특정 댓글에 좋아요 누른 멤버 조회")
    @GetMapping("/{ticketId}/comments/{commentId}/likes")
    public ApiResponse<CommentLikeListResponse> getLikingMembersList(
            @PathVariable("ticketId") Long ticketId,
            @PathVariable("commentId") Long commentId) {

        CommentLikeListResponse response = commentService.getLikingMembersList(ticketId, commentId);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @ManagerOrUser
    @Operation(summary = "API 명세서 v0.3 line 53", description = "티켓 댓글에 파일 첨부")
    @PostMapping("/{ticketId}/comments/attachment")
    public ApiResponse<CommentAttachmentResponse> uploadCommentAttachment(
            @AuthenticationPrincipal CustomUser user,
            @PathVariable("ticketId") Long ticketId,
            @RequestParam("file") MultipartFile file) {

        CommentAttachmentResponse response = commentService.uploadCommentAttachment(user, ticketId, file);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }


}
