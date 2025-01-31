package com.quartz.checkin.controller;

import com.quartz.checkin.dto.request.TicketCommentRequest;
import com.quartz.checkin.dto.response.ApiResponse;
import com.quartz.checkin.dto.response.CommentAttachmentResponse;
import com.quartz.checkin.dto.response.CommentLikeListResponse;
import com.quartz.checkin.dto.response.CommentLikeResponse;
import com.quartz.checkin.dto.response.CommentResponse;
import com.quartz.checkin.dto.response.TicketActivityResponse;
import com.quartz.checkin.security.CustomUser;
import com.quartz.checkin.security.annotation.ManagerOrUser;
import com.quartz.checkin.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    @PostMapping("/{ticketId}/comments")
    @Operation(summary = "댓글 작성", description = "특정 티켓에 댓글을 작성합니다.")
    public ApiResponse<CommentResponse> writeComment(
            @AuthenticationPrincipal CustomUser customUser,
            @Parameter(description = "티켓 ID", required = true) @PathVariable("ticketId") Long ticketId,
            @Valid @RequestBody TicketCommentRequest request) {
        return ApiResponse.createSuccessResponseWithData(200, commentService.writeComment(customUser, ticketId,
                request.getContent()));
    }

    @GetMapping("/{ticketId}/comments")
    @Operation(summary = "티켓의 댓글 및 로그 전체 조회", description = "특정 티켓의 댓글 및 로그를 전부 조회합니다.")
    public ApiResponse<TicketActivityResponse> getCommentsAndLogs(
            @Parameter(description = "티켓 ID", required = true) @PathVariable("ticketId") Long ticketId) {
        return ApiResponse.createSuccessResponseWithData(200, commentService.getCommentsAndLogs(ticketId));
    }

    @ManagerOrUser
    @PutMapping("/{ticketId}/comments/{commentId}/likes")
    @Operation(summary = "댓글 좋아요 토글", description = "특정 댓글에 좋아요를 토글합니다.")
    public ApiResponse<CommentLikeResponse> toggleLike(
            @AuthenticationPrincipal CustomUser customUser,
            @Parameter(description = "티켓 ID", required = true) @PathVariable("ticketId") Long ticketId,
            @Parameter(description = "댓글 ID", required = true) @PathVariable("commentId") Long commentId) {
        return ApiResponse.createSuccessResponseWithData(200, commentService.toggleLike(customUser, ticketId, commentId));
    }

    @GetMapping("/{ticketId}/comments/{commentId}/likes")
    @Operation(summary = "댓글 좋아요 누른 멤버 조회", description = "특정 댓글에 좋아요를 누른 멤버를 조회합니다.")
    public ApiResponse<CommentLikeListResponse> getLikingMembersList(
            @Parameter(description = "티켓 ID", required = true) @PathVariable("ticketId") Long ticketId,
            @Parameter(description = "댓글 ID", required = true) @PathVariable("commentId") Long commentId) {
        return ApiResponse.createSuccessResponseWithData(200, commentService.getLikingMembersList(ticketId, commentId));
    }

    @ManagerOrUser
    @PostMapping("/{ticketId}/comments/attachment")
    @Operation(summary = "댓글 첨부파일 업로드", description = "특정 티켓에 첨부파일을 댓글로서 업로드합니다.")
    public ApiResponse<CommentAttachmentResponse> uploadCommentAttachment(
            @AuthenticationPrincipal CustomUser customUser,
            @Parameter(description = "티켓 ID", required = true) @PathVariable("ticketId") Long ticketId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.createSuccessResponseWithData(200, commentService.uploadCommentAttachment(customUser,
                ticketId, file));
    }

}
