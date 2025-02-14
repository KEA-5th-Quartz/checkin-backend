package com.quartz.checkin.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.config.S3Config;
import com.quartz.checkin.dto.comment.response.CommentAttachmentResponse;
import com.quartz.checkin.dto.comment.response.CommentLikeListResponse;
import com.quartz.checkin.dto.comment.response.CommentLikeResponse;
import com.quartz.checkin.dto.comment.response.CommentResponse;
import com.quartz.checkin.dto.ticket.response.TicketActivityResponse;
import com.quartz.checkin.entity.BaseEntity;
import com.quartz.checkin.entity.Comment;
import com.quartz.checkin.entity.Like;
import com.quartz.checkin.entity.LogType;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Role;
import com.quartz.checkin.entity.Status;
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
import com.quartz.checkin.service.CommentService;
import com.quartz.checkin.service.S3Service;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private TicketLogRepository ticketLogRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CustomUser customUser;
    private Member member;
    private Ticket ticket;
    private Comment comment;
    private Comment savedComment;
    private MockMultipartFile file;

    @Value("${cloud.aws.projectId}")
    private String projectId;

    @Value("${cloud.aws.bucket}")
    private String bucket;

    private final String uploadedUrl = "https://objectstorage.kr-central-2.kakaocloud.com/v1/" + projectId + bucket + "/comment/1.jpg";

    @BeforeEach
    void setUp() {
        // Member 생성
        member = Member.builder()
                .id(1L)
                .username("test.user")
                .email("test@example.com")
                .password("password")
                .role(Role.USER)
                .profilePic("default.jpg")
                .build();

        // CustomUser 생성 - Member 정보 기반
        customUser = new CustomUser(
                member.getId(),
                member.getUsername(),
                member.getPassword(),
                member.getEmail(),
                member.getProfilePic(),
                member.getRole(),
                LocalDateTime.now(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + member.getRole().name()))
        );

        // Ticket 생성
        ticket = Ticket.builder()
                .customId("TEST-001")
                .user(member)
                .title("Test Ticket")
                .content("Test Content")
                .status(Status.OPEN)
                .dueDate(LocalDate.now().plusDays(7))
                .build();
        // ID는 reflection 으로 설정
        setId(ticket, 1L);

        // Comment 생성
        comment = new Comment();
        comment.setTicket(ticket);
        comment.setMember(member);
        comment.writeContent("Test comment");
        // ID는 reflection 으로 설정
        setId(comment, 1L);

        // Attachment 생성
        file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        savedComment = new Comment();
        savedComment.setTicket(ticket);
        savedComment.setMember(member);
        savedComment.writeContent(file.getContentType());
        savedComment.addAttachment(uploadedUrl);
        setId(savedComment, 2L);
    }

    // Reflection 을 사용해 private id 필드에 값을 설정
    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setCreatedAt(BaseEntity entity, LocalDateTime createdAt) {
        try {
            var field = BaseEntity.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(entity, createdAt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("댓글 작성 - 성공")
    void writeCommentSuccess() {
        // Given
        ArgumentCaptor<CommentAddedEvent> eventCaptor = ArgumentCaptor.forClass(CommentAddedEvent.class);
        when(ticketRepository.findById(any())).thenReturn(Optional.of(ticket));
        when(memberRepository.findById(any())).thenReturn(Optional.of(member));
        when(commentRepository.save(any())).thenReturn(comment);

        // When
        CommentResponse response = commentService.writeComment(customUser, ticket.getId(), "Test comment");

        // Then
        assertThat(response.getCommentId()).isEqualTo(comment.getId());
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        CommentAddedEvent capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getId()).isEqualTo(ticket.getId());
    }

    @Test
    @DisplayName("댓글 작성 - 접근 권한이 없는 경우 실패")
    void writeCommentForbiddenForNonTicketOwner() {
        // Given
        Member otherMember = Member.builder()
                .id(2L)
                .username("other.user")
                .email("other@example.com")
                .password("password")
                .role(Role.USER)
                .profilePic("default.jpg")
                .build();

        Ticket otherTicket = Ticket.builder()
                .customId("TEST-002")
                .user(otherMember)  // 다른 사용자의 티켓
                .title("Other Ticket")
                .content("Other Content")
                .status(Status.OPEN)
                .dueDate(LocalDate.now().plusDays(7))
                .build();
        setId(otherTicket, 2L);

        when(ticketRepository.findById(any())).thenReturn(Optional.of(otherTicket));
        when(memberRepository.findById(any())).thenReturn(Optional.of(member));

        // When & Then
        assertThatThrownBy(() ->
                commentService.writeComment(customUser, otherTicket.getId(), "Test comment")
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("댓글 작성 - 회원이 없는 경우 실패")
    void writeCommentMemberNotFound() {
        // Given
        when(ticketRepository.findById(any())).thenReturn(Optional.of(ticket));
        when(memberRepository.findById(any())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                commentService.writeComment(customUser, ticket.getId(), "Test comment")
        ).isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("댓글 작성 - 티켓이 없는 경우 실패")
    void writeCommentTicketNotFound() {
        // Given
        when(ticketRepository.findById(any())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                commentService.writeComment(customUser, ticket.getId(), "Test comment")
        ).isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("댓글 첨부파일 업로드 - 성공")
    void uploadCommentAttachmentSuccess() throws IOException {
        // Given
        when(ticketRepository.findById(any())).thenReturn(Optional.of(ticket));
        when(memberRepository.findById(any())).thenReturn(Optional.of(member));
        when(s3Service.uploadFile(any(), eq(S3Config.COMMENT_DIR))).thenReturn(uploadedUrl);
        when(s3Service.isImageType(any())).thenReturn(true);
        when(commentRepository.save(any())).thenReturn(savedComment);

        // When
        CommentAttachmentResponse response = commentService.uploadCommentAttachment(customUser, ticket.getId(), file);

        // Then
        assertThat(response.getCommentId()).isNotNull();
        assertThat(response.getIsImage()).isTrue();
        assertThat(response.getAttachmentUrl()).isEqualTo(uploadedUrl);
        verify(eventPublisher).publishEvent(any(FileUploadedEvent.class));
    }

    @Test
    @DisplayName("댓글 첨부파일 업로드 - 빈 파일인 경우 실패")
    void uploadCommentAttachmentEmptyFile() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "",
                "image/jpeg",
                new byte[0]
        );

        // When & Then
        assertThatThrownBy(() ->
                commentService.uploadCommentAttachment(customUser, ticket.getId(), emptyFile)
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("댓글 첨부파일 업로드 - 접근 권한이 없는 경우 실패")
    void uploadCommentAttachmentForbidden() {
        // Given
        MockMultipartFile failedFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        Member otherMember = Member.builder()
                .id(2L)
                .username("other.user")
                .email("other@example.com")
                .role(Role.USER)
                .build();

        Ticket otherTicket = Ticket.builder()
                .customId("TEST-002")
                .user(otherMember)
                .build();
        setId(otherTicket, 2L);

        String failedUrl = "https://objectstorage.kr-central-2.kakaocloud.com/v1/project-id/bucket/comment/3.jpg";

        Comment failedComment = new Comment();
        setId(failedComment, 3L);
        failedComment.setTicket(otherTicket);
        failedComment.setMember(otherMember);
        failedComment.writeContent("Test comment");
        failedComment.addAttachment(failedUrl);

        when(ticketRepository.findById(any())).thenReturn(Optional.of(otherTicket));
        when(memberRepository.findById(any())).thenReturn(Optional.of(member));

        // When & Then
        assertThatThrownBy(() ->
                commentService.uploadCommentAttachment(customUser, otherTicket.getId(), failedFile)
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("댓글 첨부파일 업로드 - 파일 크기를 초과하는 경우 실패")
    void uploadCommentAttachmentFileSizeExceeded() {
        // Given
        String exceededUrl = "https://objectstorage.kr-central-2.kakaocloud.com/v1/" + projectId + bucket + "/comment/4.jpg";
        MockMultipartFile exceededFile = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                new byte[11 * 1024 * 1024] // 11MB
        );

        Comment exceededComment = new Comment();
        setId(exceededComment, 4L);
        exceededComment.setTicket(ticket);
        exceededComment.setMember(member);
        exceededComment.writeContent(exceededFile.getContentType());
        exceededComment.addAttachment(exceededUrl);

        // When & Then
        assertThatThrownBy(() ->
                commentService.uploadCommentAttachment(customUser, ticket.getId(), exceededFile)
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.TOO_LARGE_FILE));
    }

    @Test
    @DisplayName("댓글 첨부파일 업로드 - 미존재 티켓인 경우 실패")
    void uploadCommentAttachmentTicketNotFound() {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        when(ticketRepository.findById(any())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                commentService.uploadCommentAttachment(customUser, 999L, file)
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.TICKET_NOT_FOUND));
    }

    @Test
    @DisplayName("댓글 첨부파일 업로드 - 스토리지 오류가 발생하는 경우 실패")
    void uploadCommentAttachmentStorageError() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        when(ticketRepository.findById(any())).thenReturn(Optional.of(ticket));
        when(memberRepository.findById(any())).thenReturn(Optional.of(member));
        when(s3Service.uploadFile(any(), any())).thenThrow(new RuntimeException("Storage error"));

        // When & Then
        assertThatThrownBy(() ->
                commentService.uploadCommentAttachment(customUser, ticket.getId(), file)
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.OBJECT_STORAGE_ERROR));
    }

    @Test
    @DisplayName("댓글과 로그 목록 조회 - 성공")
    void getCommentsAndLogsSuccess() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        setCreatedAt(comment, now);
        TicketLog log = TicketLog.builder()
                .ticket(ticket)
                .logType(LogType.STATUS)
                .content("Test log")
                .createdAt(now.plusDays(1))
                .build();
        setId(log, 1L);
        setCreatedAt(log, now.plusDays(1));

        when(ticketRepository.existsById(any())).thenReturn(true);
        when(ticketLogRepository.findByTicketId(any())).thenReturn(List.of(log));
        when(commentRepository.findByTicketId(any())).thenReturn(List.of(comment));

        // When
        TicketActivityResponse response = commentService.getCommentsAndLogs(ticket.getId());

        // Then
        assertThat(response.getActivities()).hasSize(2); // 1 log + 1 comment
        assertThat(response.getId()).isEqualTo(ticket.getId());
    }

    @Test
    @DisplayName("댓글과 로그 목록 조회 - 티켓이 없는 경우 실패")
    void getCommentsAndLogsTicketNotFound() {
        // Given
        when(ticketRepository.existsById(any())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() ->
                commentService.getCommentsAndLogs(ticket.getId())
        ).isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("댓글 좋아요 토글 - 좋아요 추가")
    void toggleLikeAdd() {
        // Given
        Like like = new Like();
        setId(like, 1L);
        like.setComment(comment);
        like.setMember(member);

        when(ticketRepository.existsById(any())).thenReturn(true);
        when(likeRepository.existsByCommentIdAndMemberId(any(), any())).thenReturn(false);
        when(commentRepository.findById(any())).thenReturn(Optional.of(comment));
        when(memberRepository.findById(any())).thenReturn(Optional.of(member));
        when(likeRepository.save(any())).thenReturn(like);

        // When
        CommentLikeResponse response = commentService.toggleLike(customUser, ticket.getId(), comment.getId());

        // Then
        assertThat(response.getIsLiked()).isTrue();
        assertThat(response.getLikeId()).isEqualTo(like.getId());
    }

    @Test
    @DisplayName("댓글 좋아요 토글 - 좋아요 취소")
    void toggleLikeRemove() {
        // Given
        Like like = new Like();
        setId(like, 1L);
        like.setComment(comment);
        like.setMember(member);

        when(ticketRepository.existsById(any())).thenReturn(true);
        when(likeRepository.existsByCommentIdAndMemberId(any(), any())).thenReturn(true);
        when(likeRepository.getLikeByCommentIdAndMemberId(any(), any())).thenReturn(like);

        // When
        CommentLikeResponse response = commentService.toggleLike(customUser, ticket.getId(), comment.getId());

        // Then
        assertThat(response.getIsLiked()).isFalse();
        verify(likeRepository).deleteLikeById(like.getId());
    }

    @Test
    @DisplayName("댓글 좋아요 토글 - 회원이 없는 경우 실패")
    void toggleLikeMemberNotFound() {
        // Given
        when(ticketRepository.existsById(any())).thenReturn(true);
        when(commentRepository.findById(any())).thenReturn(Optional.of(comment));
        when(memberRepository.findById(any())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                commentService.toggleLike(customUser, ticket.getId(), comment.getId())
        ).isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("댓글 좋아요 토글 - 티켓이 없는 경우 실패")
    void toggleLikeTicketNotFound() {
        // Given
        when(ticketRepository.existsById(any())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() ->
                commentService.toggleLike(customUser, ticket.getId(), comment.getId())
        ).isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("댓글 좋아요 토글 - 댓글이 없는 경우 실패")
    void toggleLikeCommentNotFound() {
        // Given
        when(ticketRepository.existsById(any())).thenReturn(true);
        when(commentRepository.findById(any())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                commentService.toggleLike(customUser, ticket.getId(), comment.getId())
        ).isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("댓글 좋아요 목록 조회 - 성공")
    void getLikingMembersListSuccess() {
        // Given
        Like like = new Like();
        setId(like, 1L);
        like.setComment(comment);
        like.setMember(member);

        when(ticketRepository.existsById(any())).thenReturn(true);
        when(commentRepository.existsById(any())).thenReturn(true);
        when(likeRepository.getLikesByCommentId(any())).thenReturn(List.of(like));

        // When
        CommentLikeListResponse response = commentService.getLikingMembersList(ticket.getId(), comment.getId());

        // Then
        assertThat(response.getTotalLikes()).isEqualTo(1);
        assertThat(response.getLikes()).hasSize(1);
        assertThat(response.getLikes().get(0).getUsername()).isEqualTo(member.getUsername());
    }

    @Test
    @DisplayName("댓글 좋아요 목록 조회 - 티켓이 없는 경우 실패")
    void getLikingMembersListTicketNotFound() {
        // Given
        when(ticketRepository.existsById(any())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() ->
                commentService.getLikingMembersList(ticket.getId(), comment.getId())
        ).isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("댓글 좋아요 목록 조회 - 댓글이 없는 경우 실패")
    void getLikingMembersListCommentNotFound() {
        // Given
        when(ticketRepository.existsById(any())).thenReturn(true);
        when(commentRepository.existsById(any())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() ->
                commentService.getLikingMembersList(ticket.getId(), comment.getId())
        ).isInstanceOf(ApiException.class);
    }
}