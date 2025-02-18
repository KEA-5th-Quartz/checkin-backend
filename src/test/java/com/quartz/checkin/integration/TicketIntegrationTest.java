package com.quartz.checkin.integration;

import static com.quartz.checkin.util.ErrorResponseMatchers.errorResponse;
import static com.quartz.checkin.util.RequestAuthPostProcessor.authenticatedAsManager;
import static com.quartz.checkin.util.RequestAuthPostProcessor.authenticatedAsUser;
import static com.quartz.checkin.util.RequestAuthPostProcessor.authenticatedCustom;
import static com.quartz.checkin.util.RequestAuthPostProcessor.getProperty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.category.request.FirstCategoryPatchRequest;
import com.quartz.checkin.dto.category.request.SecondCategoryPatchRequest;
import com.quartz.checkin.dto.ticket.request.PriorityUpdateRequest;
import com.quartz.checkin.dto.ticket.request.TicketCreateRequest;
import com.quartz.checkin.dto.ticket.request.TicketDeleteOrRestoreOrPurgeRequest;
import com.quartz.checkin.dto.ticket.request.TicketUpdateRequest;
import com.quartz.checkin.entity.Priority;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.repository.TicketRepository;
import com.quartz.checkin.service.AttachmentService;
import com.quartz.checkin.service.S3Service;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpMethod;
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
public class TicketIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private S3Service s3Service;

    @MockitoBean
    private AttachmentService attachmentService;

    @MockitoBean
    private ApplicationEventPublisher applicationEventPublisher;

    private TicketCreateRequest ticketCreateRequest;
    private TicketUpdateRequest ticketUpdateRequest;
    private TicketDeleteOrRestoreOrPurgeRequest ticketDeleteRequest;
    private MockMultipartFile file1;
    private MockMultipartFile file2;

    @BeforeEach
    void setUp() {
        ticketCreateRequest = new TicketCreateRequest("티켓 제목", "티켓 내용",
                "VM", "생성", LocalDate.now(), new ArrayList<Long>());
        ticketUpdateRequest = new TicketUpdateRequest("티켓 제목 수정", "티켓 내용 수정",
                "VM", "생성", LocalDate.now(), new ArrayList<Long>());

        file1 = new MockMultipartFile(
                "files",
                "test.txt",
                "text/plain",
                "test data".getBytes());

        file2 = new MockMultipartFile(
                "files",
                "test2.txt",
                "text/plain",
                "test data2".getBytes());
    }

    private void setTicketDeleteRequest(Object entity, List<Long> ids) {
        try {
            var field = entity.getClass().getDeclaredField("ticketIds");
            field.setAccessible(true);
            field.set(entity, ids);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setFirstCategoryPatchRequest(Object entity, String name) {
        try {
            var field = entity.getClass().getDeclaredField("firstCategory");
            field.setAccessible(true);
            field.set(entity, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setSecondCategoryPatchRequest(Object entity, String name) {
        try {
            var field = entity.getClass().getDeclaredField("secondCategory");
            field.setAccessible(true);
            field.set(entity, name);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setPriorityUpdateRequest(Object entity, Priority priority) {
        try {
            var field = entity.getClass().getDeclaredField("priority");
            field.setAccessible(true);
            field.set(entity, priority);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("티켓 생성 성공")
    public void createTicketSuccess() throws Exception {
        mockMvc.perform(post("/tickets")
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketCreateRequest)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("티켓 생성 실패 - 접근  권한 누락")
    public void createTicketFailUnauthorized() throws Exception {
        mockMvc.perform(post("/tickets")
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketCreateRequest)))
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("티켓 생성 실패 - 로그인 요구")
    public void createTicketFailForbidden() throws Exception {
        mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketCreateRequest)))
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    @DisplayName("티켓 생성 실패 - 제목 누락")
    public void createTicketFailNoTitle() throws Exception {
        TicketCreateRequest modifiedRequest = new TicketCreateRequest(
                "",
                ticketCreateRequest.getContent(),
                ticketCreateRequest.getFirstCategory(),
                ticketCreateRequest.getSecondCategory(),
                ticketCreateRequest.getDueDate(),
                ticketCreateRequest.getAttachmentIds()
        );

        mockMvc.perform(post("/tickets")
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modifiedRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("티켓 생성 실패 - 내용 누락")
    public void createTicketFailNoContent() throws Exception {
        TicketCreateRequest modifiedRequest = new TicketCreateRequest(
                ticketCreateRequest.getTitle(),
                "",
                ticketCreateRequest.getFirstCategory(),
                ticketCreateRequest.getSecondCategory(),
                ticketCreateRequest.getDueDate(),
                ticketCreateRequest.getAttachmentIds()
        );

        mockMvc.perform(post("/tickets")
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modifiedRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("티켓 생성 실패 - 제목 비유효")
    public void createTicketFailInvalidTitle() throws Exception {
        TicketCreateRequest modifiedRequest = new TicketCreateRequest(
                "a".repeat(101),
                ticketCreateRequest.getContent(),
                ticketCreateRequest.getFirstCategory(),
                ticketCreateRequest.getSecondCategory(),
                ticketCreateRequest.getDueDate(),
                ticketCreateRequest.getAttachmentIds()
        );

        mockMvc.perform(post("/tickets")
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modifiedRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("티켓 생성 실패 - 내용 비유효")
    public void createTicketFailInvalidContent() throws Exception {
        TicketCreateRequest modifiedRequest = new TicketCreateRequest(
                ticketCreateRequest.getTitle(),
                "a".repeat(1001),
                ticketCreateRequest.getFirstCategory(),
                ticketCreateRequest.getSecondCategory(),
                ticketCreateRequest.getDueDate(),
                ticketCreateRequest.getAttachmentIds()
        );

        mockMvc.perform(post("/tickets")
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modifiedRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("티켓 생성 실패 - 마감 기한 비유효")
    public void createTicketFailInvalidDueDate() throws Exception {
        TicketCreateRequest modifiedRequest = new TicketCreateRequest(
                ticketCreateRequest.getTitle(),
                ticketCreateRequest.getContent(),
                ticketCreateRequest.getFirstCategory(),
                ticketCreateRequest.getSecondCategory(),
                LocalDate.now().minusDays(1),
                ticketCreateRequest.getAttachmentIds()
        );

        mockMvc.perform(post("/tickets")
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modifiedRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("티켓 생성 실패 - 미존재 1차 카테고리")
    public void createTicketFailNoFirstCategory() throws Exception {
        TicketCreateRequest modifiedRequest = new TicketCreateRequest(
                ticketCreateRequest.getTitle(),
                ticketCreateRequest.getContent(),
                "미존재",
                ticketCreateRequest.getSecondCategory(),
                ticketCreateRequest.getDueDate(),
                ticketCreateRequest.getAttachmentIds()
        );

        mockMvc.perform(post("/tickets")
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modifiedRequest)))
            .andExpect(status().isNotFound())
            .andExpect(errorResponse(ErrorCode.CATEGORY_NOT_FOUND_FIRST));
    }

    @Test
    @DisplayName("티켓 생성 실패 - 1차 카테고리 미기입")
    public void createTicketFailNoFirstCategoryInput() throws Exception {
        TicketCreateRequest modifiedRequest = new TicketCreateRequest(
                ticketCreateRequest.getTitle(),
                ticketCreateRequest.getContent(),
                "",
                ticketCreateRequest.getSecondCategory(),
                ticketCreateRequest.getDueDate(),
                ticketCreateRequest.getAttachmentIds()
        );

        mockMvc.perform(post("/tickets")
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modifiedRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("티켓 생성 실패 - 미존재 2차 카테고리")
    public void createTicketFailNoSecondCategory() throws Exception {
        TicketCreateRequest modifiedRequest = new TicketCreateRequest(
                ticketCreateRequest.getTitle(),
                ticketCreateRequest.getContent(),
                ticketCreateRequest.getFirstCategory(),
                "미존재",
                ticketCreateRequest.getDueDate(),
                ticketCreateRequest.getAttachmentIds()
        );

        mockMvc.perform(post("/tickets")
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modifiedRequest)))
            .andExpect(status().isNotFound())
            .andExpect(errorResponse(ErrorCode.CATEGORY_NOT_FOUND_SECOND));
    }

    @Test
    @DisplayName("티켓 생성 실패 - 2차 카테고리 미기입")
    public void createTicketFailNoSecondCategoryInput() throws Exception {
        TicketCreateRequest modifiedRequest = new TicketCreateRequest(
                ticketCreateRequest.getTitle(),
                ticketCreateRequest.getContent(),
                ticketCreateRequest.getFirstCategory(),
                "",
                ticketCreateRequest.getDueDate(),
                ticketCreateRequest.getAttachmentIds()
        );

        mockMvc.perform(post("/tickets")
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modifiedRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("티켓 생성 실패 - 유효하지 않은 첨부파일 첨부")
    public void createTicketFailInvalidAttachment() throws Exception {
        TicketCreateRequest modifiedRequest = new TicketCreateRequest(
                ticketCreateRequest.getTitle(),
                ticketCreateRequest.getContent(),
                ticketCreateRequest.getFirstCategory(),
                ticketCreateRequest.getSecondCategory(),
                ticketCreateRequest.getDueDate(),
                new ArrayList<Long>() {{ add(4L); }}
        );

        mockMvc.perform(post("/tickets")
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modifiedRequest)))
            .andExpect(status().isNotFound())
            .andExpect(errorResponse(ErrorCode.ATTACHMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("티켓 첨부파일 업로드 성공")
    public void uploadTicketAttachmentSuccess() throws Exception {
        MockHttpServletRequestBuilder request = multipart(HttpMethod.POST, "/tickets/attachment")
            .file(file1)
            .file(file2)
            .with(authenticatedAsUser(mockMvc))
            .contentType(MediaType.MULTIPART_FORM_DATA);

        mockMvc.perform(request)
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("티켓 첨부파일 업로드 실패 - 필수 데이터 누락")
    public void uploadTicketAttachmentFailNoFile() throws Exception {
        mockMvc.perform(multipart(HttpMethod.POST, "/tickets/attachment")
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("티켓 첨부파일 업로드 실패 - 접근 권한 누락")
    public void uploadTicketAttachmentFailUnauthorized() throws Exception {
        MockHttpServletRequestBuilder request = multipart(HttpMethod.POST, "/tickets/attachment")
            .file(file1)
            .file(file2)
            .with(authenticatedAsManager(mockMvc))
            .contentType(MediaType.MULTIPART_FORM_DATA);

        mockMvc.perform(request)
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("티켓 수정 성공")
    public void updateTicketSuccess() throws Exception {
        mockMvc.perform(put("/tickets/{ticketId}", 2L)
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketUpdateRequest)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("티켓 수정 실패 - 담당이 아닌 담당자가 수정")
    public void updateTicketFailUnauthorizedManager() throws Exception {
        mockMvc.perform(put("/tickets/{ticketId}", 2L)
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketCreateRequest)))
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("티켓 수정 실패 - 접근 권한 누락(다른 사용자)")
    public void updateTicketFailUnauthorized() throws Exception {
        String password = getProperty("test.login.user").split(",")[1].split(":")[1].split("}")[0];
        password = password.replaceAll("[\\s\"]", "");

        System.out.println(password);
        mockMvc.perform(put("/tickets/{ticketId}", 2L)
                .with(authenticatedCustom(mockMvc, "user.a", password))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketCreateRequest)))
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("티켓 수정 실패 - 로그인 요구")
    public void updateTicketFailForbidden() throws Exception {
        mockMvc.perform(put("/tickets/{ticketId}", 2L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketCreateRequest)))
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    @DisplayName("티켓 수정 실패 - 제목 누락")
    public void updateTicketFailNoTitle() throws Exception {
        TicketUpdateRequest modifiedRequest = new TicketUpdateRequest(
                "",
                ticketUpdateRequest.getContent(),
                ticketUpdateRequest.getFirstCategory(),
                ticketUpdateRequest.getSecondCategory(),
                ticketUpdateRequest.getDueDate(),
                ticketUpdateRequest.getAttachmentIds()
        );

        mockMvc.perform(put("/tickets/{ticketId}", 2L)
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modifiedRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("티켓 수정 실패 - 제목 비유효")
    public void updateTicketFailInvalidTitle() throws Exception {
        TicketUpdateRequest modifiedRequest = new TicketUpdateRequest(
                "a".repeat(101),
                ticketUpdateRequest.getContent(),
                ticketUpdateRequest.getFirstCategory(),
                ticketUpdateRequest.getSecondCategory(),
                ticketUpdateRequest.getDueDate(),
                ticketUpdateRequest.getAttachmentIds()
        );

        mockMvc.perform(put("/tickets/{ticketId}", 2L)
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modifiedRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("티켓 수정 실패 - 내용 누락")
    public void updateTicketFailNoContent() throws Exception {
        TicketUpdateRequest modifiedRequest = new TicketUpdateRequest(
                ticketUpdateRequest.getTitle(),
                "",
                ticketUpdateRequest.getFirstCategory(),
                ticketUpdateRequest.getSecondCategory(),
                ticketUpdateRequest.getDueDate(),
                ticketUpdateRequest.getAttachmentIds()
        );

        mockMvc.perform(put("/tickets/{ticketId}", 2L)
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modifiedRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("티켓 수정 실패 - 내용 비유효")
    public void updateTicketFailInvalidContent() throws Exception {
        TicketUpdateRequest modifiedRequest = new TicketUpdateRequest(
                ticketUpdateRequest.getTitle(),
                "a".repeat(1001),
                ticketUpdateRequest.getFirstCategory(),
                ticketUpdateRequest.getSecondCategory(),
                ticketUpdateRequest.getDueDate(),
                ticketUpdateRequest.getAttachmentIds()
        );

        mockMvc.perform(put("/tickets/{ticketId}", 2L)
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modifiedRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("티켓 수정 실패 - 마감 기한 비유효")
    public void updateTicketFailInvalidDueDate() throws Exception {
        TicketUpdateRequest modifiedRequest = new TicketUpdateRequest(
                ticketUpdateRequest.getTitle(),
                ticketUpdateRequest.getContent(),
                ticketUpdateRequest.getFirstCategory(),
                ticketUpdateRequest.getSecondCategory(),
                LocalDate.now().minusDays(1),
                ticketUpdateRequest.getAttachmentIds()
        );

        mockMvc.perform(put("/tickets/{ticketId}", 2L)
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(modifiedRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("티켓 수정 실패 - 미존재 티켓")
    public void updateTicketFailNoTicket() throws Exception {
        mockMvc.perform(put("/tickets/{ticketId}", 1000L)
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketUpdateRequest)))
            .andExpect(status().isNotFound())
            .andExpect(errorResponse(ErrorCode.TICKET_NOT_FOUND));
    }

    @Test
    @DisplayName("티켓 임시 삭제 성공")
    public void deleteTicketSuccess() throws Exception {
        ticketDeleteRequest = new TicketDeleteOrRestoreOrPurgeRequest();
        setTicketDeleteRequest(ticketDeleteRequest, new ArrayList<Long>() {{ add(131L); add(136L); }});

        mockMvc.perform(patch("/tickets")
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketDeleteRequest)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("티켓 임시 삭제 실패 - 접근 권한 누락")
    public void deleteTicketFailUnauthorized() throws Exception {
        ticketDeleteRequest = new TicketDeleteOrRestoreOrPurgeRequest();
        setTicketDeleteRequest(ticketDeleteRequest, new ArrayList<Long>() {{ add(131L); add(136L); }});

        mockMvc.perform(patch("/tickets")
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketDeleteRequest)))
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("티켓 임시 삭제 실패 - 진행 중인 티켓 삭제")
    public void deleteTicketFailDeleteInProgress() throws Exception {
        ticketDeleteRequest = new TicketDeleteOrRestoreOrPurgeRequest();
        setTicketDeleteRequest(ticketDeleteRequest, new ArrayList<Long>() {{ add(2L); }});

        mockMvc.perform(patch("/tickets")
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ticketDeleteRequest)))
            .andExpect(status().isConflict())
            .andExpect(errorResponse(ErrorCode.TICKET_ALREADY_ASSIGNED));
    }

    @Test
    @DisplayName("티켓 상세 조회 성공")
    public void getTicketDetailSuccess() throws Exception {
        mockMvc.perform(get("/tickets/{ticketId}", 2L)
                .with(authenticatedAsUser(mockMvc)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("티켓 상세 조회 실패 - 접근 권한 누락")
    public void getTicketDetailFailUnauthorized() throws Exception {
        String password = getProperty("test.login.user").split(",")[1].split(":")[1].split("}")[0];
        password = password.replaceAll("[\\s\"]", "");

        mockMvc.perform(get("/tickets/{ticketId}", 2L)
                .with(authenticatedCustom(mockMvc, "user.b", password)))
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("티켓 상세 조회 실패 - 미존재 티켓")
    public void getTicketDetailFailNoTicket() throws Exception {
        mockMvc.perform(get("/tickets/{ticketId}", 1000L)
                .with(authenticatedAsUser(mockMvc)))
            .andExpect(status().isNotFound())
            .andExpect(errorResponse(ErrorCode.TICKET_NOT_FOUND));
    }

    @Test
    @DisplayName("전체 담당자 티켓 목록 조회 성공")
    public void getTicketListSuccess() throws Exception {
        mockMvc.perform(get("/tickets")
                .with(authenticatedAsManager(mockMvc)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("전체 담당자 티켓 목록 조회 성공 - 상태 필터링")
    public void getTicketListSuccessFilterStatus() throws Exception {
        mockMvc.perform(get("/tickets")
                .param("statuses", "IN_PROGRESS")
                .with(authenticatedAsManager(mockMvc)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("전체 담당자 티켓 목록 조회 성공 - 사용자 필터링")
    public void getTicketListSuccessFilterUser() throws Exception {
        mockMvc.perform(get("/tickets")
                .param("username", "user.a")
                .with(authenticatedAsManager(mockMvc)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("전체 담당자 티켓 목록 조회 성공 - 카테고리 필터링")
    public void getTicketListSuccessFilterCategory() throws Exception {
        mockMvc.perform(get("/tickets")
                .param("categories", "개발")
                .with(authenticatedAsManager(mockMvc)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("전체 담당자 티켓 목록 조회 성공 - 중요도 필터링")
    public void getTicketListSuccessFilterPriority() throws Exception {
        mockMvc.perform(get("/tickets")
                .param("priorities", "EMERGENCY")
                .with(authenticatedAsManager(mockMvc)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("전체 담당자 티켓 목록 조회 성공 - 오늘 마감")
    public void getTicketListSuccessDueToday() throws Exception {
        mockMvc.perform(get("/tickets")
                .param("dueToday", "true")
                .with(authenticatedAsManager(mockMvc)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("전체 담당자 티켓 목록 조회 성공 - 이번 주 마감")
    public void getTicketListSuccessDueThisWeek() throws Exception {
        mockMvc.perform(get("/tickets")
                .param("dueThisWeek", "true")
                .with(authenticatedAsManager(mockMvc)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("전체 담당자 티켓 목록 조회 성공 - 정렬")
    public void getTicketListSuccessSort() throws Exception {
        mockMvc.perform(get("/tickets")
                .param("page", "1")
                .param("size", "20")
                .param("sortByCreatedAt", "asc")
                .with(authenticatedAsManager(mockMvc)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("전체 담당자 티켓 목록 조회 실패 - 접근 권한 누락")
    public void getTicketListFailUnauthorized() throws Exception {
        mockMvc.perform(get("/tickets"))
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    @DisplayName("전체 담당자 티켓 목록 조회 실패 - 접근 권한 누락(사용자)")
    public void getTicketListFailUnauthorizedUser() throws Exception {
        mockMvc.perform(get("/tickets")
                .with(authenticatedAsUser(mockMvc)))
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.FORBIDDEN));
    }



    @Test
    @DisplayName("전체 담당자 티켓 목록 조회 실패 - 페이지 번호 비유효")
    public void getTicketListFailInvalidPage() throws Exception {
        mockMvc.perform(get("/tickets")
                .param("page", "0")
                .with(authenticatedAsManager(mockMvc)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_PAGE_NUMBER));
    }

    @Test
    @DisplayName("전체 담당자 티켓 목록 조회 실패 - 페이지 크기 비유효")
    public void getTicketListFailInvalidSize() throws Exception {
        mockMvc.perform(get("/tickets")
                .param("size", "0")
                .with(authenticatedAsManager(mockMvc)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_PAGE_SIZE));
    }

    @Test
    @DisplayName("전체 담당자 티켓 목록 조회 실패 - 페이지 크기 비유효(최대)")
    public void getTicketListFailInvalidSizeMax() throws Exception {
        mockMvc.perform(get("/tickets")
                .param("size", "101")
                .with(authenticatedAsManager(mockMvc)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_PAGE_SIZE));
    }

    @Test
    @DisplayName("전체 담당자 티켓 목록 조회 실패 - 유효하지 않은 필터값")
    public void getTicketListFailInvalidFilter() throws Exception {
        mockMvc.perform(get("/tickets")
                        .param("statuses", "INVALID_STATUS")
                        .with(authenticatedAsManager(mockMvc)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(errorResponse(ErrorCode.METHOD_NOT_ALLOWED));
    }

    @Test
    @DisplayName("전체 담당자 티켓 목록 조회 실패 - 유효하지 않은 정렬 기준")
    public void getTicketListFailInvalidSortDirection() throws Exception {
        mockMvc.perform(get("/tickets")
                        .param("sortByCreatedAt", "INVALID_DIRECTION")
                        .with(authenticatedAsManager(mockMvc)))
                .andExpect(status().isBadRequest())
                .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("사용자 티켓 목록 조회 성공")
    public void getMyTicketListSuccess() throws Exception {
        mockMvc.perform(get("/tickets/my-tickets")
                .with(authenticatedAsUser(mockMvc)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("사용자 티켓 목록 조회 성공 - 상태 필터링")
    public void getMyTicketListSuccessFilterStatus() throws Exception {
        mockMvc.perform(get("/tickets/my-tickets")
                .param("statuses", "IN_PROGRESS")
                .with(authenticatedAsUser(mockMvc)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("사용자 티켓 목록 조회 성공 - 사용자 필터링")
    public void getMyTicketListSuccessFilterUser() throws Exception {
        mockMvc.perform(get("/tickets/my-tickets")
                .param("username", "user.a")
                .with(authenticatedAsUser(mockMvc)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("사용자 티켓 목록 조회 성공 - 카테고리 필터링")
    public void getMyTicketListSuccessFilterCategory() throws Exception {
        mockMvc.perform(get("/tickets/my-tickets")
                .param("categories", "개발")
                .with(authenticatedAsUser(mockMvc)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("사용자 티켓 목록 조회 성공 - 중요도 필터링")
    public void getMyTicketListSuccessFilterPriority() throws Exception {
        mockMvc.perform(get("/tickets/my-tickets")
                .param("priorities", "EMERGENCY")
                .with(authenticatedAsUser(mockMvc)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("사용자 티켓 목록 조회 성공 - 오늘 마감")
    public void getMyTicketListSuccessDueToday() throws Exception {
        mockMvc.perform(get("/tickets/my-tickets")
                .param("dueToday", "true")
                .with(authenticatedAsUser(mockMvc)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("사용자 티켓 목록 조회 성공 - 이번 주 마감")
    public void getMyTicketListSuccessDueThisWeek() throws Exception {
        mockMvc.perform(get("/tickets/my-tickets")
                .param("dueThisWeek", "true")
                .with(authenticatedAsUser(mockMvc)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("사용자 티켓 목록 조회 성공 - 정렬")
    public void getMyTicketListSuccessSort() throws Exception {
        mockMvc.perform(get("/tickets/my-tickets")
                .param("page", "1")
                .param("size", "20")
                .param("sortByDueDate", "asc")
                .with(authenticatedAsUser(mockMvc)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("사용자 티켓 목록 조회 실패 - 접근 권한 누락")
    public void getMyTicketListFailUnauthorized() throws Exception {
        mockMvc.perform(get("/tickets/my-tickets"))
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    @DisplayName("사용자 티켓 목록 조회 실패 - 유효하지 않은 필터값")
    public void getMyTicketListFailInvalidFilter() throws Exception {
        mockMvc.perform(get("/tickets/my-tickets")
                        .param("statuses", "INVALID_STATUS")
                        .with(authenticatedAsUser(mockMvc)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(errorResponse(ErrorCode.METHOD_NOT_ALLOWED));
    }

    @Test
    @DisplayName("사용자 티켓 목록 조회 실패 - 유효하지 않은 정렬 기준")
    public void getMyTicketListFailInvalidSortDirection() throws Exception {
        mockMvc.perform(get("/tickets/my-tickets")
                        .param("sortByCreatedAt", "INVALID_DIRECTION")
                        .with(authenticatedAsUser(mockMvc)))
                .andExpect(status().isBadRequest())
                .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("사용자 티켓 목록 조회 실패 - 비유효 페이지 번호")
    public void getMyTicketListFailInvalidPage() throws Exception {
        mockMvc.perform(get("/tickets/my-tickets")
                .param("page", "0")
                .with(authenticatedAsUser(mockMvc)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_PAGE_NUMBER));
    }

    @Test
    @DisplayName("사용자 티켓 목록 조회 실패 - 비유효 페이지 크기")
    public void getMyTicketListFailInvalidSize() throws Exception {
        mockMvc.perform(get("/tickets/my-tickets")
                .param("size", "0")
                .with(authenticatedAsUser(mockMvc)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_PAGE_SIZE));
    }

    @Test
    @DisplayName("사용자 티켓 목록 조회 실패 - 비유효 페이지 크기(최대)")
    public void getMyTicketListFailInvalidSizeMax() throws Exception {
        mockMvc.perform(get("/tickets/my-tickets")
                .param("size", "101")
                .with(authenticatedAsUser(mockMvc)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_PAGE_SIZE));
    }

    @Test
    @DisplayName("티켓 상태 완료로 변경 성공")
    public void closeTicketSuccess() throws Exception {
        mockMvc.perform(patch("/tickets/{ticketId}/close", 1L)
                .with(authenticatedAsManager(mockMvc)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("티켓 상태 완료로 변경 실패 - 티켓이 이미 완료됨")
    public void closeTicketFailAlreadyClosed() throws Exception {
        mockMvc.perform(patch("/tickets/{ticketId}/close", 88L)
                .with(authenticatedAsManager(mockMvc)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.CANNOT_CHANGE_COMPLETED_TICKET));
    }

    @Test
    @DisplayName("티켓 상태 완료로 변경 실패 - 접근 권한 누락")
    public void closeTicketFailUnauthorized() throws Exception {
        mockMvc.perform(patch("/tickets/{ticketId}/close", 2L))
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    @DisplayName("티켓 상태 완료로 변경 실패 - 담당자 불일치")
    public void closeTicketFailMismacheddManager() throws Exception {
        mockMvc.perform(patch("/tickets/{ticketId}/close", 2L)
                .with(authenticatedAsManager(mockMvc)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_TICKET_MANAGER));
    }

    @Test
    @DisplayName("티켓 상태 완료로 변경 실패 - 접근 권한 누락(사용자)")
    public void closeTicketFailUnauthorizedUser() throws Exception {
        mockMvc.perform(patch("/tickets/{ticketId}/close", 2L)
                .with(authenticatedAsUser(mockMvc)))
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("티켓 상태 완료로 변경 실패 - 티켓이 진행중이 아님")
    public void closeTicketFailNotInProgress() throws Exception {
        mockMvc.perform(patch("/tickets/{ticketId}/close", 84L)
                .with(authenticatedAsManager(mockMvc)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_TICKET_STATUS));
    }

    @Test
    @DisplayName("티켓 상태 완료로 변경 실패 - 미존재 티켓")
    public void closeTicketFailNoTicket() throws Exception {
        mockMvc.perform(patch("/tickets/{ticketId}/close", 1000L)
                .with(authenticatedAsManager(mockMvc)))
            .andExpect(status().isNotFound())
            .andExpect(errorResponse(ErrorCode.TICKET_NOT_FOUND));
    }

    @Test
    @DisplayName("티켓 1차 카테고리 변경 성공")
    public void changeFirstCategorySuccess() throws Exception {
        FirstCategoryPatchRequest firstCategoryPatchRequest = new FirstCategoryPatchRequest();
        setFirstCategoryPatchRequest(firstCategoryPatchRequest, "LB");

        mockMvc.perform(patch("/tickets/{ticketId}/category", 1L)
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstCategoryPatchRequest)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("티켓 1차 카테고리 변경 실패 - 접근 권한 누락")
    public void changeFirstCategoryFailUnauthorized() throws Exception {
        FirstCategoryPatchRequest firstCategoryPatchRequest = new FirstCategoryPatchRequest();
        setFirstCategoryPatchRequest(firstCategoryPatchRequest, "LB");

        mockMvc.perform(patch("/tickets/{ticketId}/category", 2L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstCategoryPatchRequest)))
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    @DisplayName("티켓 1차 카테고리 변경 실패 - 접근 권한 누락(사용자)")
    public void changeFirstCategoryFailUnauthorizedUser() throws Exception {
        FirstCategoryPatchRequest firstCategoryPatchRequest = new FirstCategoryPatchRequest();
        setFirstCategoryPatchRequest(firstCategoryPatchRequest, "LB");

        mockMvc.perform(patch("/tickets/{ticketId}/category", 2L)
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstCategoryPatchRequest)))
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("티켓 1차 카테고리 변경 실패 - 미존재 티켓")
    public void changeFirstCategoryFailNoTicket() throws Exception {
        FirstCategoryPatchRequest firstCategoryPatchRequest = new FirstCategoryPatchRequest();
        setFirstCategoryPatchRequest(firstCategoryPatchRequest, "LB");

        mockMvc.perform(patch("/tickets/{ticketId}/category", 1000L)
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstCategoryPatchRequest)))
            .andExpect(status().isNotFound())
            .andExpect(errorResponse(ErrorCode.TICKET_NOT_FOUND));
    }

    @Test
    @DisplayName("티켓 1차 카테고리 변경 실패 - 미존재 1차 카테고리")
    public void changeFirstCategoryFailNoFirstCategory() throws Exception {
        FirstCategoryPatchRequest firstCategoryPatchRequest = new FirstCategoryPatchRequest();
        setFirstCategoryPatchRequest(firstCategoryPatchRequest, "미존재");

        mockMvc.perform(patch("/tickets/{ticketId}/category", 1L)
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstCategoryPatchRequest)))
            .andExpect(status().isNotFound())
            .andExpect(errorResponse(ErrorCode.CATEGORY_NOT_FOUND_FIRST));
    }

    @Test
    @DisplayName("티켓 1차 카테고리 변경 실패 - 1차 카테고리 미기입")
    public void changeFirstCategoryFailNoFirstCategoryInput() throws Exception {
        FirstCategoryPatchRequest firstCategoryPatchRequest = new FirstCategoryPatchRequest();
        setFirstCategoryPatchRequest(firstCategoryPatchRequest, "");

        mockMvc.perform(patch("/tickets/{ticketId}/category", 2L)
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstCategoryPatchRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("티켓 2차 카테고리 변경 성공")
    public void changeSecondCategorySuccess() throws Exception {
        SecondCategoryPatchRequest secondCategoryPatchRequest = new SecondCategoryPatchRequest();
        setSecondCategoryPatchRequest(secondCategoryPatchRequest, "기타");

        mockMvc.perform(patch("/tickets/{ticketId}/category/{fisrtCategoryId}", 1L, 6L)
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondCategoryPatchRequest)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("티켓 2차 카테고리 변경 실패 - 접근 권한 누락")
    public void changeSecondCategoryFailUnauthorized() throws Exception {
        SecondCategoryPatchRequest secondCategoryPatchRequest = new SecondCategoryPatchRequest();
        setSecondCategoryPatchRequest(secondCategoryPatchRequest, "변경");

        mockMvc.perform(patch("/tickets/{ticketId}/category/{fisrtCategoryId}", 2L, 6L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondCategoryPatchRequest)))
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    @DisplayName("티켓 2차 카테고리 변경 실패 - 접근 권한 누락(사용자)")
    public void changeSecondCategoryFailUnauthorizedUser() throws Exception {
        SecondCategoryPatchRequest secondCategoryPatchRequest = new SecondCategoryPatchRequest();
        setSecondCategoryPatchRequest(secondCategoryPatchRequest, "변경");

        mockMvc.perform(patch("/tickets/{ticketId}/category/{fisrtCategoryId}", 1L, 6L)
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondCategoryPatchRequest)))
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("티켓 2차 카테고리 변경 실패 - 미존재 티켓")
    public void changeSecondCategoryFailNoTicket() throws Exception {
        SecondCategoryPatchRequest secondCategoryPatchRequest = new SecondCategoryPatchRequest();
        setSecondCategoryPatchRequest(secondCategoryPatchRequest, "변경");

        mockMvc.perform(patch("/tickets/{ticketId}/category/{fisrtCategoryId}", 1000L, 6L)
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondCategoryPatchRequest)))
            .andExpect(status().isNotFound())
            .andExpect(errorResponse(ErrorCode.TICKET_NOT_FOUND));
    }

    @Test
    @DisplayName("티켓 2차 카테고리 변경 실패 - 미존재 2차 카테고리")
    public void changeSecondCategoryFailNoSecondCategory() throws Exception {
        SecondCategoryPatchRequest secondCategoryPatchRequest = new SecondCategoryPatchRequest();
        setSecondCategoryPatchRequest(secondCategoryPatchRequest, "미존재");

        mockMvc.perform(patch("/tickets/{ticketId}/category/{fisrtCategoryId}", 1L, 6L)
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondCategoryPatchRequest)))
            .andExpect(status().isNotFound())
            .andExpect(errorResponse(ErrorCode.CATEGORY_NOT_FOUND_SECOND));
    }

    @Test
    @DisplayName("티켓 2차 카테고리 변경 실패 - 2차 카테고리 미기입")
    public void changeSecondCategoryFailNoSecondCategoryInput() throws Exception {
        SecondCategoryPatchRequest secondCategoryPatchRequest = new SecondCategoryPatchRequest();
        setSecondCategoryPatchRequest(secondCategoryPatchRequest, "");

        mockMvc.perform(patch("/tickets/{ticketId}/category/{fisrtCategoryId}", 1L, 6L)
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondCategoryPatchRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("티켓 2차 카테고리 변경 실패 - 미존재 1차 카테고리")
    public void changeSecondCategoryFailNoFirstCategory() throws Exception {
        SecondCategoryPatchRequest secondCategoryPatchRequest = new SecondCategoryPatchRequest();
        setSecondCategoryPatchRequest(secondCategoryPatchRequest, "변경");

        mockMvc.perform(patch("/tickets/{ticketId}/category/{fisrtCategoryId}", 1L, 100L)
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondCategoryPatchRequest)))
            .andExpect(status().isNotFound())
            .andExpect(errorResponse(ErrorCode.CATEGORY_NOT_FOUND_FIRST));
    }

    @Test
    @DisplayName("티켓 담당자 변경 성공 - 담당자 없음")
    public void reassignManagerSuccess() throws Exception {
        String request = """
                {
                  "manager": "king.hj"
                }""";
        mockMvc.perform(patch("/tickets/{ticketId}/assign", 84L)
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("티켓 담당자 변경 성공 - 다른 담당자에게 넘기기")
    public void reassignManagerSuccessInProgress() throws Exception {
        String request = """
                {
                  "manager": "scarlett.kim"
                }""";
        mockMvc.perform(patch("/tickets/{ticketId}/assign", 1L)
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("티켓 담당자 변경 실패 - 접근 권한 누락")
    public void reassignManagerFailUnauthorized() throws Exception {
        String request = """
                {
                  "manager": "king.hj"
                }""";
        mockMvc.perform(patch("/tickets/{ticketId}/assign", 84L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    @DisplayName("티켓 담당자 변경 실패 - 접근 권한 누락(사용자)")
    public void reassignManagerFailUnauthorizedUser() throws Exception {
        String request = """
                {
                  "manager": "king.hj"
                }""";
        mockMvc.perform(patch("/tickets/{ticketId}/assign", 84L)
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("티켓 담당자 변경 실패 - 미존재 티켓")
    public void reassignManagerFailNoTicket() throws Exception {
        String request = """
                {
                  "manager": "king.hj"
                }""";
        mockMvc.perform(patch("/tickets/{ticketId}/assign", 1000L)
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isNotFound())
            .andExpect(errorResponse(ErrorCode.TICKET_NOT_FOUND));
    }

    @Test
    @DisplayName("티켓 담당자 변경 실패 - 미존재 담당자")
    public void reassignManagerFailManagerNotFound() throws Exception {
        String request = """
                {
                  "manager": "no.one"
                }""";
        mockMvc.perform(patch("/tickets/{ticketId}/assign", 84L)
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isNotFound())
            .andExpect(errorResponse(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    @DisplayName("티켓 담당자 변경 실패 - 담당자 누락")
    public void reassignManagerFailNoManagerInput() throws Exception {
        String request = "";
        mockMvc.perform(patch("/tickets/{ticketId}/assign", 84L)
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("티켓 담당자 변경 실패 - 진행 중인 티켓 변경 시도")
    public void reassignManagerFailInProgress() throws Exception {
        String request = """
                {
                  "manager": "king.hj"
                }""";
        mockMvc.perform(patch("/tickets/{ticketId}/assign", 2L)
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isConflict())
            .andExpect(errorResponse(ErrorCode.TICKET_ALREADY_ASSIGNED));
    }

    @Test
    @DisplayName("티켓 담당자 변경 실패 - 담당자 본인을 할당")
    public void reassignManagerFailSelf() throws Exception {
        String request = """
                {
                  "manager": "king.hj"
                }""";
        mockMvc.perform(patch("/tickets/{ticketId}/assign", 1L)
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isConflict())
            .andExpect(errorResponse(ErrorCode.TICKET_ALREADY_ASSIGNED_TO_SELF));
    }

    @Test
    @DisplayName("티켓 중요도 변경 성공")
    public void changePrioritySuccess() throws Exception {
        PriorityUpdateRequest priorityPatchRequest = new PriorityUpdateRequest();
        setPriorityUpdateRequest(priorityPatchRequest, Priority.EMERGENCY);

        mockMvc.perform(patch("/tickets/{ticketId}/priority", 1L)
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(priorityPatchRequest)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("티켓 중요도 변경 실패 - 접근 권한 누락")
    public void changePriorityFailUnauthorized() throws Exception {
        PriorityUpdateRequest priorityPatchRequest = new PriorityUpdateRequest();
        setPriorityUpdateRequest(priorityPatchRequest, Priority.EMERGENCY);

        mockMvc.perform(patch("/tickets/{ticketId}/priority", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(priorityPatchRequest)))
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    @DisplayName("티켓 중요도 변경 실패 - 접근 권한 누락(사용자)")
    public void changePriorityFailUnauthorizedUser() throws Exception {
        PriorityUpdateRequest priorityPatchRequest = new PriorityUpdateRequest();
        setPriorityUpdateRequest(priorityPatchRequest, Priority.EMERGENCY);

        mockMvc.perform(patch("/tickets/{ticketId}/priority", 1L)
                .with(authenticatedAsUser(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(priorityPatchRequest)))
            .andExpect(status().isForbidden())
            .andExpect(errorResponse(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("티켓 중요도 변경 실패 - 미존재 티켓")
    public void changePriorityFailNoTicket() throws Exception {
        PriorityUpdateRequest priorityPatchRequest = new PriorityUpdateRequest();
        setPriorityUpdateRequest(priorityPatchRequest, Priority.EMERGENCY);

        mockMvc.perform(patch("/tickets/{ticketId}/priority", 1000L)
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(priorityPatchRequest)))
            .andExpect(status().isNotFound())
            .andExpect(errorResponse(ErrorCode.TICKET_NOT_FOUND));
    }

    @Test
    @DisplayName("티켓 중요도 변경 실패 - 중요도 미기입")
    public void changePriorityFailNoPriorityInput() throws Exception {
        PriorityUpdateRequest priorityPatchRequest = new PriorityUpdateRequest();
        setPriorityUpdateRequest(priorityPatchRequest, null);

        mockMvc.perform(patch("/tickets/{ticketId}/priority", 1L)
                .with(authenticatedAsManager(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(priorityPatchRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }
}
