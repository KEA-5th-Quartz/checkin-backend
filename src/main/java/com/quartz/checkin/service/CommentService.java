package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.config.S3Config;
import com.quartz.checkin.dto.response.ActivityResponse;
import com.quartz.checkin.dto.response.ActivityType;
import com.quartz.checkin.dto.response.CommentAttachmentResponse;
import com.quartz.checkin.dto.response.CommentLikeListResponse;
import com.quartz.checkin.dto.response.CommentLikeResponse;
import com.quartz.checkin.dto.response.CommentResponse;
import com.quartz.checkin.dto.response.LikesUserList;
import com.quartz.checkin.dto.response.TicketActivityResponse;
import com.quartz.checkin.entity.Comment;
import com.quartz.checkin.entity.Like;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Role;
import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.entity.TicketLog;
import com.quartz.checkin.repository.CommentRepository;
import com.quartz.checkin.repository.LikeRepository;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.repository.TicketLogRepository;
import com.quartz.checkin.repository.TicketRepository;
import com.quartz.checkin.security.CustomUser;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class CommentService {
    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final TicketLogRepository ticketLogRepository;
    private final LikeRepository likeRepository;
    private final MemberRepository memberRepository;

    private final S3UploadService s3UploadService;

    /**
     * 회원이 작성한 댓글을 저장한다.
     *
     * @param user     사용자 정보
     * @param ticketId 티켓 ID
     * @param content  댓글 내용
     * @return 댓글 ID
     */
    @Transactional
    public CommentResponse writeComment(CustomUser user, Long ticketId, String content) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> {
                    log.error(ErrorCode.TICKET_NOT_FOUND.getMessage());
                    return new ApiException(ErrorCode.TICKET_NOT_FOUND);
                });
        Member member = memberRepository.findById(user.getId())
                .orElseThrow(() -> {
                    log.error(ErrorCode.MEMBER_NOT_FOUND.getMessage());
                    return new ApiException(ErrorCode.MEMBER_NOT_FOUND);
                });

        // 해당 "사용자"가 생성한 티켓이 아닌 경우 댓글 작성 불가
        if (user.getRole() == Role.USER && !ticket.getUser().getId().equals(user.getId())) {
            log.error(ErrorCode.FORBIDDEN.getMessage());
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setMember(member);
        comment.writeContent(content);

        Comment savedComment = commentRepository.save(comment);
        return CommentResponse.builder()
                .commentId(savedComment.getId())
                .build();
    }

    /**
     * 티켓에 작성된 댓글과 로그를 조회한다.
     *
     * @param ticketId 티켓 ID
     * @return 댓글과 로그
     */
    public TicketActivityResponse getCommentsAndLogs(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            log.error(ErrorCode.TICKET_NOT_FOUND.getMessage());
            throw new ApiException(ErrorCode.TICKET_NOT_FOUND);
        }

        List<TicketLog> logs = ticketLogRepository.findByTicketId(ticketId);
        List<Comment> comments = commentRepository.findByTicketId(ticketId);

        List<ActivityResponse> activities = Stream.concat(
                        logs.stream().map(this::convertLogToActivity),
                        comments.stream().map(this::convertCommentToActivity)
                ).sorted(Comparator.comparing(ActivityResponse::getCreatedAt))
                .collect(Collectors.toList());

        return TicketActivityResponse.builder()
                .ticketId(ticketId)
                .activities(activities)
                .build();
    }

    /**
     * TicketLog을 ActivityResponse로 변환한다.
     *
     * @param log 변환할 TicketLog
     * @return 변환된 ActivityResponse
     */
    private ActivityResponse convertLogToActivity(TicketLog log) {
        return ActivityResponse.builder()
                .type(ActivityType.LOG)
                .createdAt(log.getCreatedAt())
                .logId(log.getId())
                .logContent(log.getContent())
                .build();
    }

    /**
     * Comment를 ActivityResponse로 변환한다.
     *
     * @param comment 변환할 Comment
     * @return 변환된 ActivityResponse
     */
    private ActivityResponse convertCommentToActivity(Comment comment) {
        if (comment.getAttachment() != null) {
            return ActivityResponse.builder()
                    .type(ActivityType.COMMENT)
                    .createdAt(comment.getCreatedAt())
                    .commentId(comment.getId())
                    .memberId(comment.getMember().getId())
                    .isImage(s3UploadService.isImageType(comment.getContent()))
                    .attachmentUrl(comment.getAttachment())
                    .build();
        } else {
            return ActivityResponse.builder()
                    .type(ActivityType.COMMENT)
                    .createdAt(comment.getCreatedAt())
                    .commentId(comment.getId())
                    .memberId(comment.getMember().getId())
                    .commentContent(comment.getContent())
                    .build();
        }
    }

    /**
     * 댓글에 좋아요를 토글한다.<br>
     * 이미 좋아요를 누른 경우 좋아요를 취소한다.
     *
     * @param user      사용자 정보
     * @param ticketId  티켓 ID
     * @param commentId 댓글 ID
     * @return 좋아요 여부, 좋아요 ID, 댓글 ID
     */
    @Transactional
    public CommentLikeResponse toggleLike(CustomUser user, Long ticketId, Long commentId) {
        if (!ticketRepository.existsById(ticketId)) {
            log.error(ErrorCode.TICKET_NOT_FOUND.getMessage());
            throw new ApiException(ErrorCode.TICKET_NOT_FOUND);
        }

        // 이미 좋아요를 "누른" 경우 좋아요를 "취소"함
        if (likeRepository.existsByCommentIdAndMemberId(commentId, user.getId())) {
            likeRepository.deleteLikeById(likeRepository.getLikeByCommentIdAndMemberId(commentId, user.getId()).getId());
            return CommentLikeResponse.builder()
                    .isLiked(false)
                    .commentId(commentId)
                    .build();
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.error(ErrorCode.COMMENT_NOT_FOUND.getMessage());
                    return new ApiException(ErrorCode.COMMENT_NOT_FOUND);
                });
        Member member = memberRepository.findById(user.getId())
                .orElseThrow(() -> {
                    log.error(ErrorCode.MEMBER_NOT_FOUND.getMessage());
                    return new ApiException(ErrorCode.MEMBER_NOT_FOUND);
                });

        Like like = new Like();
        like.setComment(comment);
        like.setMember(member);

        Like savedLike = likeRepository.save(like);
        return CommentLikeResponse.builder()
                .isLiked(true)
                .likeId(savedLike.getId())
                .commentId(commentId)
                .build();
    }

    /**
     * 댓글에 좋아요를 누른 회원 목록을 조회한다.
     *
     * @param ticketId  티켓 ID
     * @param commentId 댓글 ID
     * @return 좋아요 누른 회원 목록
     */
    public CommentLikeListResponse getLikingMembersList(Long ticketId, Long commentId) {
        if (!ticketRepository.existsById(ticketId)) {
            log.error(ErrorCode.TICKET_NOT_FOUND.getMessage());
            throw new ApiException(ErrorCode.TICKET_NOT_FOUND);
        }

        if (!commentRepository.existsById(commentId)) {
            log.error(ErrorCode.COMMENT_NOT_FOUND.getMessage());
            throw new ApiException(ErrorCode.COMMENT_NOT_FOUND);
        }

        List<LikesUserList> likes = likeRepository.getLikesByCommentId(commentId).stream().map(like -> LikesUserList.builder()
                .memberId(like.getMember().getId())
                .username(like.getMember().getUsername())
                .build()).toList();

        return CommentLikeListResponse.builder()
                .ticketId(ticketId)
                .commentId(commentId)
                .totalLikes(likes.size())
                .likes(likes)
                .build();
    }

    @Transactional
    public CommentAttachmentResponse uploadCommentAttachment(CustomUser user, Long ticketId, MultipartFile file) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> {
                    log.error(ErrorCode.TICKET_NOT_FOUND.getMessage());
                    return new ApiException(ErrorCode.TICKET_NOT_FOUND);
                });

        Member member = memberRepository.findById(user.getId())
                .orElseThrow(() -> {
                    log.error(ErrorCode.MEMBER_NOT_FOUND.getMessage());
                    return new ApiException(ErrorCode.MEMBER_NOT_FOUND);
                });



        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setMember(member);
        comment.writeContent(file.getContentType());

        try {
            String attachmentUrl = s3UploadService.uploadFile(file, S3Config.COMMENT_DIR);
            comment.addAttachment(attachmentUrl);
            Comment savedComment = commentRepository.save(comment);

            return CommentAttachmentResponse.builder()
                    .commentId(savedComment.getId())
                    .isImage(s3UploadService.isImageType(file.getContentType()))
                    .attachmentUrl(attachmentUrl)
                    .build();
        } catch (Exception e) {
            log.error("S3에 댓글용 첨부파일을 업로드할 수 없습니다. {}", e.getMessage());
            throw new ApiException(ErrorCode.OBJECT_STORAGE_ERROR);
        }

    }

}
