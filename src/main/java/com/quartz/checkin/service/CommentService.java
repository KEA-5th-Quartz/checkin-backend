package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.config.S3Config;
import com.quartz.checkin.dto.comment.response.CommentAttachmentResponse;
import com.quartz.checkin.dto.comment.response.CommentLikeListResponse;
import com.quartz.checkin.dto.comment.response.CommentLikeResponse;
import com.quartz.checkin.dto.comment.response.CommentResponse;
import com.quartz.checkin.dto.comment.response.LikesUserList;
import com.quartz.checkin.dto.ticket.response.ActivityResponse;
import com.quartz.checkin.dto.ticket.response.ActivityType;
import com.quartz.checkin.dto.ticket.response.TicketActivityResponse;
import com.quartz.checkin.entity.Comment;
import com.quartz.checkin.entity.Like;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Role;
import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.entity.TicketLog;
import com.quartz.checkin.event.CommentAddedEvent;
import com.quartz.checkin.event.FileUploadedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
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
    private final WebhookService webhookService;
    private final ApplicationEventPublisher eventPublisher;

    private final S3Service s3Service;

    @Transactional
    public CommentResponse writeComment(CustomUser user, Long ticketId, String content) {
        TicketAndMember ticketAndMember = findTicketAndMember(user, ticketId);

        Comment comment = new Comment();
        comment.updateTicket(ticketAndMember.ticket());
        comment.updateMember(ticketAndMember.member());
        comment.writeContent(content);

        Comment savedComment = commentRepository.save(comment);

        eventPublisher.publishEvent(new CommentAddedEvent(
                ticketAndMember.ticket().getId(),
                ticketAndMember.ticket().getCustomId(),
                ticketAndMember.ticket().getAgitId(),
                comment));

        return CommentResponse.builder()
                .commentId(savedComment.getId())
                .build();
    }

    @Transactional
    public CommentAttachmentResponse uploadCommentAttachment(CustomUser user, Long ticketId, MultipartFile file) {
        if (file.isEmpty()) {
            log.error("첨부된 파일을 찾을 수 없습니다. {}", file.getOriginalFilename());
            throw new ApiException(ErrorCode.INVALID_DATA);
        }

        TicketAndMember ticketAndMember = findTicketAndMember(user, ticketId);

        Comment comment = new Comment();
        comment.updateTicket(ticketAndMember.ticket());
        comment.updateMember(ticketAndMember.member());
        comment.writeContent(file.getContentType());

        try {
            String attachmentUrl = s3Service.uploadFile(file, S3Config.COMMENT_DIR);
            comment.addAttachment(attachmentUrl);
            Comment savedComment = commentRepository.save(comment);

            eventPublisher.publishEvent(new FileUploadedEvent(
                    ticketAndMember.ticket().getId(),
                    ticketAndMember.ticket().getCustomId(),
                    ticketAndMember.ticket().getAgitId(),
                    ticketAndMember.member().getUsername()
            ));

            return CommentAttachmentResponse.builder()
                    .commentId(savedComment.getId())
                    .isImage(s3Service.isImageType(file.getContentType()))
                    .attachmentUrl(attachmentUrl)
                    .build();
        } catch (Exception e) {
            log.error("Object Storage에 댓글용 첨부파일을 업로드할 수 없습니다. {}", e.getMessage());
            throw new ApiException(ErrorCode.OBJECT_STORAGE_ERROR);
        }
    }

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
                .id(ticketId)
                .activities(activities)
                .build();
    }

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
    public CommentLikeResponse toggleLike(CustomUser user, Long ticketId, Long commentId) {
        if (!ticketRepository.existsById(ticketId)) {
            log.error(ErrorCode.TICKET_NOT_FOUND.getMessage());
            throw new ApiException(ErrorCode.TICKET_NOT_FOUND);
        }

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
        like.updateComment(comment);
        like.updateMember(member);

        Like savedLike = likeRepository.save(like);
        return CommentLikeResponse.builder()
                .isLiked(true)
                .likeId(savedLike.getId())
                .commentId(commentId)
                .build();
    }

    private ActivityResponse convertLogToActivity(TicketLog log) {
        return ActivityResponse.builder()
                .type(ActivityType.LOG)
                .createdAt(log.getCreatedAt())
                .logId(log.getId())
                .logContent(log.getContent())
                .build();
    }

    private ActivityResponse convertCommentToActivity(Comment comment) {
        if (comment.getAttachment() != null) {
            return ActivityResponse.builder()
                    .type(ActivityType.COMMENT)
                    .createdAt(comment.getCreatedAt())
                    .commentId(comment.getId())
                    .memberId(comment.getMember().getId())
                    .isImage(s3Service.isImageType(comment.getContent()))
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

    private TicketAndMember findTicketAndMember(CustomUser user, Long ticketId) {
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

        if (user.getRole() == Role.USER && !ticket.getUser().getId().equals(user.getId())) {
            log.error(ErrorCode.FORBIDDEN.getMessage());
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        return new TicketAndMember(ticket, member);
    }

    private record TicketAndMember(Ticket ticket, Member member) {}

}