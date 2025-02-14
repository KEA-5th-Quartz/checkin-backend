package com.quartz.checkin.integration;

import static com.quartz.checkin.util.ApiResponseMatchers.apiResponse;
import static com.quartz.checkin.util.ErrorResponseMatchers.errorResponse;
import static com.quartz.checkin.util.RequestAuthPostProcessor.authenticatedAsUser;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.repository.CommentRepository;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.repository.TicketRepository;
import com.quartz.checkin.service.S3Service;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles(value = "test")
@AutoConfigureMockMvc
@SpringBootTest
@Sql(scripts = "classpath:data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Transactional
public class CommentIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private S3Service s3Service; // 파일 업로드 mocking

    @MockitoBean
    private ApplicationEventPublisher eventPublisher; // 이벤트 발행 mocking

    @Test
    @DisplayName("댓글 작성 성공")
    public void writeCommentSuccess() throws Exception {
        String commentRequestJson = """
           {
               "content": "테스트 댓글입니다."
           }
           """;

        mockMvc.perform(post("/tickets/{ticketId}/comments", 2L)
                        .with(authenticatedAsUser(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentRequestJson))
                .andExpect(status().isOk())
                .andExpect(apiResponse(HttpStatus.OK.value(),
                        Map.of("commentId", notNullValue())));
    }

    @Test
    @DisplayName("댓글 작성 실패 - 접근 권한이 없는 경우")
    public void writeCommentFailForbidden() throws Exception {
        String commentRequestJson = """
           {
               "content": "테스트 댓글입니다."
           }
           """;

        mockMvc.perform(post("/tickets/{ticketId}/comments", 1L)
                        .with(authenticatedAsUser(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentRequestJson))
                .andExpect(status().isForbidden())
                .andExpect(errorResponse(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("댓글 작성 실패 - 존재하지 않는 티켓이 요청된 경우")
    public void writeCommentFailTicketNotFound() throws Exception {
        String commentRequestJson = """
           {
               "content": "테스트 댓글입니다."
           }
           """;

        mockMvc.perform(post("/tickets/{ticketId}/comments", 999L)
                        .with(authenticatedAsUser(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentRequestJson))
                .andExpect(status().isNotFound())
                .andExpect(errorResponse(ErrorCode.TICKET_NOT_FOUND));
    }

    @Test
    @DisplayName("첨부파일 댓글 작성 성공")
    public void writeCommentWithAttachmentSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test data".getBytes());

        MockHttpServletRequestBuilder requestBuilder = multipart(HttpMethod.POST, "/tickets/{ticketId}/comments/attachment", 2L)
                .file(file)
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .content(file.getBytes());

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(apiResponse(HttpStatus.OK.value(),
                    Map.of("commentId", notNullValue())));
    }

    @Test
    @DisplayName("첨부파일 댓글 작성 실패 - 첨부파일이 빈 파일인 경우")
    public void writeCommentWithAttachmentFailEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                new byte[0]);

        MockHttpServletRequestBuilder requestBuilder = multipart(HttpMethod.POST, "/tickets/{ticketId}/comments/attachment", 2L)
                .file(file)
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .content(file.getBytes());

        mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("첨부파일 댓글 작성 실패 - 접근 권한이 없는 경우")
    public void writeCommentWithAttachmentFailForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test data".getBytes());

        MockHttpServletRequestBuilder requestBuilder = multipart(HttpMethod.POST, "/tickets/{ticketId}/comments/attachment", 1L)
                .file(file)
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .content(file.getBytes());

        mockMvc.perform(requestBuilder)
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("첨부파일 댓글 작성 실패 - 파일 크기가 10MB를 초과하는 경우")
    public void writeCommentWithAttachmentFailFileTooLarge() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                new byte[1024 * 1024 * 11]);

        MockHttpServletRequestBuilder requestBuilder = multipart(HttpMethod.POST, "/tickets/{ticketId}/comments/attachment", 2L)
                .file(file)
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .content(file.getBytes());

        mockMvc.perform(requestBuilder)
            .andExpect(status().isPayloadTooLarge())
            .andExpect(errorResponse(ErrorCode.TOO_LARGE_FILE));
    }

    @Test
    @DisplayName("첨부파일 댓글 작성 실패 - 존재하지 않는 티켓이 요청된 경우")
    public void writeCommentWithAttachmentFailTicketNotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "test data".getBytes());

        MockHttpServletRequestBuilder requestBuilder = multipart(HttpMethod.POST, "/tickets/{ticketId}/comments/attachment", 99999L)
                .file(file)
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .content(file.getBytes());

        mockMvc.perform(requestBuilder)
            .andExpect(status().isNotFound())
            .andExpect(errorResponse(ErrorCode.TICKET_NOT_FOUND));
    }

    @Test
    @DisplayName("티켓 및 로그 조회 성공")
    public void getTicketAndLogsSuccess() throws Exception {
        mockMvc.perform(get("/tickets/{ticketId}/comments", 2L)
                        .with(authenticatedAsUser(mockMvc)))
                .andExpect(status().isOk())
                .andExpect(apiResponse(HttpStatus.OK.value(),
                        Map.of("activities", notNullValue())));
    }

    @Test
    @DisplayName("티켓 및 로그 조회 실패 - 티켓이 존재하지 않는 경우")
    public void getTicketAndLogsFailTicketNotFound() throws Exception {
        mockMvc.perform(get("/tickets/{ticketId}/comments", 999L)
                        .with(authenticatedAsUser(mockMvc)))
                .andExpect(status().isNotFound())
                .andExpect(errorResponse(ErrorCode.TICKET_NOT_FOUND));
    }

    @Test
    @DisplayName("댓글 좋아요 추가 성공")
    public void likeCommentAppendSuccess() throws Exception {
        mockMvc.perform(put("/tickets/{ticketId}/comments/{commentId}/likes", 2L, 1L)
                        .with(authenticatedAsUser(mockMvc)))
                .andExpect(status().isOk())
                .andExpect(apiResponse(HttpStatus.OK.value(),
                        Map.of("commentId", notNullValue())));
    }

    @Test
    @DisplayName("댓글 좋아요 제거 성공")
    public void likeCommentRemoveSuccess() throws Exception {
        mockMvc.perform(put("/tickets/{ticketId}/comments/{commentId}/likes", 2L, 1L)
                        .with(authenticatedAsUser(mockMvc)))
                .andExpect(status().isOk())
                .andExpect(apiResponse(HttpStatus.OK.value(),
                        Map.of("commentId", notNullValue())));
    }

    @Test
    @DisplayName("댓글 좋아요 토글 실패 - 요청한 티켓이 존재하지 않는 경우")
    public void likeCommentFailCommentNotFound() throws Exception {
        mockMvc.perform(put("/tickets/{ticketId}/comments/{commentId}/likes", 999L, 1)
                        .with(authenticatedAsUser(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(errorResponse(ErrorCode.TICKET_NOT_FOUND));
    }

    @Test
    @DisplayName("댓글 좋아요 토글 실패 - 요청한 댓글이 존재하지 않는 경우")
    public void likeCommentFailTicketNotFound() throws Exception {
        mockMvc.perform(put("/tickets/{ticketId}/comments/{commentId}/likes", 2L, 999L)
                        .with(authenticatedAsUser(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(errorResponse(ErrorCode.COMMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("댓글 좋아요 목록 조회 성공")
    public void getCommentLikesSuccess() throws Exception {
        mockMvc.perform(get("/tickets/{ticketId}/comments/{commentId}/likes", 2L, 1L)
                        .with(authenticatedAsUser(mockMvc)))
                .andExpect(status().isOk())
                .andExpect(apiResponse(HttpStatus.OK.value(),
                        Map.of("likes", notNullValue())));
    }

    @Test
    @DisplayName("댓글 좋아요 목록 조회 실패 - 요청한 티켓이 존재하지 않는 경우")
    public void getCommentLikesFailTicketNotFound() throws Exception {
        mockMvc.perform(get("/tickets/{ticketId}/comments/{commentId}/likes", 999L, 1L)
                        .with(authenticatedAsUser(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(errorResponse(ErrorCode.TICKET_NOT_FOUND));
    }

    @Test
    @DisplayName("댓글 좋아요 목록 조회 실패 - 요청한 댓글이 존재하지 않는 경우")
    public void getCommentLikesFailCommentNotFound() throws Exception {
        mockMvc.perform(get("/tickets/{ticketId}/comments/{commentId}/likes", 2L, 999L)
                        .with(authenticatedAsUser(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(errorResponse(ErrorCode.COMMENT_NOT_FOUND));
    }

}
