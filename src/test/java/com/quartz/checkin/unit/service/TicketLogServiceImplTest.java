package com.quartz.checkin.unit.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.category.request.FirstCategoryPatchRequest;
import com.quartz.checkin.dto.category.request.SecondCategoryPatchRequest;
import com.quartz.checkin.entity.BaseEntity;
import com.quartz.checkin.entity.Category;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Priority;
import com.quartz.checkin.entity.Role;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.repository.CategoryRepository;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.repository.TicketLogRepository;
import com.quartz.checkin.repository.TicketRepository;
import com.quartz.checkin.service.TicketLogServiceImpl;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TicketLogServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketLogRepository ticketLogRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TicketLogServiceImpl ticketLogService;

    private Member mockUser;
    private Member mockManager;
    private Member mockManager2;
    private Category mockFirstCategory;
    private Category mockSecondCategory;
    private Category mockFirstCategory2;
    private Category mockSecondCategory2;
    private Ticket mockTicket;
    private Ticket mockTicket2;
    private FirstCategoryPatchRequest request1stCategory;
    private SecondCategoryPatchRequest request2ndCategory;

    @BeforeEach
    void setUp() {
        mockUser = Member.builder()
                .id(1L)
                .username("testUser")
                .email("test@example.com")
                .password("securePassword")
                .role(Role.USER)
                .build();

        mockManager = Member.builder()
                .id(2L)
                .username("testManager")
                .email("test2@example.com")
                .password("securePassword")
                .role(Role.MANAGER)
                .build();

        mockManager2 = Member.builder()
                .id(3L)
                .username("testManager2")
                .email("test4@example.com")
                .password("securePassword")
                .role(Role.MANAGER)
                .build();

        mockFirstCategory = new Category(null, "DevOps", "D", "Development and Operations Guide");
        setId(mockFirstCategory, 1L);
        mockSecondCategory = new Category(mockFirstCategory, "Infrastructure", "I", "Infrastructure Management Guide");
        setId(mockSecondCategory, 2L);
        mockFirstCategory2 = new Category(null, "ChangedCategory1", "C1", "Testing Cate1 Replacement");
        setId(mockFirstCategory2, 3L);
        mockSecondCategory2 = new Category(mockFirstCategory2, "ChangedCategory2", "C2", "Testing Cate2 Replacement");
        setId(mockSecondCategory2, 4L);

        mockTicket = Ticket.builder()
                .customId("0212D-I001")
                .user(mockUser)
                .firstCategory(mockFirstCategory)
                .secondCategory(mockSecondCategory)
                .title("Test Ticket")
                .content("This is a test ticket.")
                .priority(Priority.UNDEFINED)
                .status(Status.IN_PROGRESS)
                .dueDate(LocalDate.now().plusDays(1))
                .build();
        setId(mockTicket, 1L);
        setCreatedAt(mockTicket, LocalDateTime.now());
        mockTicket.assignManager(mockManager);

        mockTicket2 = Ticket.builder()
                .customId("0212D-I001")
                .user(mockUser)
                .firstCategory(mockFirstCategory2)
                .secondCategory(mockSecondCategory2)
                .title("Test Ticket")
                .content("This is a test ticket.")
                .priority(Priority.UNDEFINED)
                .status(Status.IN_PROGRESS)
                .dueDate(LocalDate.now().plusDays(1))
                .build();
        mockTicket2.assignManager(mockManager);
        setId(mockTicket2, 2L);
        setCreatedAt(mockTicket2, LocalDateTime.now());

        request1stCategory = new FirstCategoryPatchRequest();
        setFirstCategoryPatchRequest(request1stCategory, "ChangedCategory1");

        request2ndCategory = new SecondCategoryPatchRequest();
        setSecondCategoryPatchRequest(request2ndCategory, "DuplicateCategory");
    }

    private void setFirstCategoryPatchRequest(FirstCategoryPatchRequest req, String newFirsstCategory) {
        try {
            var field = FirstCategoryPatchRequest.class.getDeclaredField("firstCategory");
            field.setAccessible(true);
            field.set(req, newFirsstCategory);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setSecondCategoryPatchRequest(SecondCategoryPatchRequest req, String newSecondCategory) {
        try {
            var field = SecondCategoryPatchRequest.class.getDeclaredField("secondCategory");
            field.setAccessible(true);
            field.set(req, newSecondCategory);
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

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("티켓 상태 완료로 변경 성공")
    void closeTicketSuccess() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(java.util.Optional.of(mockTicket));
        when(memberRepository.findById(2L)).thenReturn(java.util.Optional.of(mockManager));

        // When
        ticketLogService.closeTicket(2L, 1L);

        // Then
        assertEquals(Status.CLOSED, mockTicket.getStatus());
    }

    @Test
    @DisplayName("티켓 상태 완료로 변경 실패 - 티켓이 이미 완료됨")
    void closeTicketFailTicketAlreadyClosed() {
        // Given
        mockTicket.closeTicket();
        when(memberRepository.findById(2L)).thenReturn(java.util.Optional.of(mockManager));
        when(ticketRepository.findById(1L)).thenReturn(java.util.Optional.of(mockTicket));

        // When & Then
        assertThatThrownBy(() ->
                ticketLogService.closeTicket(2L, 1L)
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.CANNOT_CHANGE_COMPLETED_TICKET));
    }

    @Test
    @DisplayName("티켓 상태 완료로 변경 실패 - 티켓이 진행 중이 아님")
    void closeTicketFailInvalidTicketStatus() {
        // Given
        Ticket mockTicket2 = Ticket.builder()
                .customId("0212D-I001")
                .user(mockUser)
                .firstCategory(mockFirstCategory)
                .secondCategory(mockSecondCategory)
                .title("Test Ticket")
                .content("This is a test ticket.")
                .priority(Priority.UNDEFINED)
                .status(Status.OPEN)
                .dueDate(LocalDate.now().plusDays(1))
                .build();
        when(memberRepository.findById(2L)).thenReturn(java.util.Optional.of(mockManager));
        when(ticketRepository.findById(1L)).thenReturn(java.util.Optional.of(mockTicket2));

        // When & Then
        assertThatThrownBy(() ->
                ticketLogService.closeTicket(2L, 1L)
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.INVALID_TICKET_STATUS));
    }

    @Test
    @DisplayName("티켓 상태 완료로 변경 실패 - 담당자 정보가 티켓과 일치하지 않음")
    void closeTicketFailTicketAlreadyAssignedToSelf() {
        // Given
        when(memberRepository.findById(3L)).thenReturn(java.util.Optional.of(mockManager2));
        when(ticketRepository.findById(1L)).thenReturn(java.util.Optional.of(mockTicket));

        // When & Then
        assertThatThrownBy(() ->
                ticketLogService.closeTicket(3L, 1L)
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.INVALID_TICKET_MANAGER));
    }

    @Test
    @DisplayName("티켓 상태 완료로 변경 실패 - 티켓이 존재하지 않음")
    void closeTicketFailTicketNotFound() {
        // When & Then
        assertThatThrownBy(() ->
                ticketLogService.closeTicket(2L, 1L)
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.TICKET_NOT_FOUND));

    }

    @Test
    @DisplayName("티켓 상태 완료로 변경 실패 - 담당자가 존재하지 않음")
    void closeTicketFailManagerNotFound() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(java.util.Optional.of(mockTicket));

        // When & Then
        assertThatThrownBy(() ->
                ticketLogService.closeTicket(5L, 1L)
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    @DisplayName("담당자가 1차 카테고리 변경 성공")
    void changeFirstCategorySuccess() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(java.util.Optional.of(mockTicket));
        when(memberRepository.findById(2L)).thenReturn(java.util.Optional.of(mockManager));
        when(categoryRepository.findByNameAndParentIsNull("ChangedCategory1")).thenReturn(java.util.Optional.of(mockFirstCategory2));
        when(categoryRepository.findByParentOrderByIdAsc(mockFirstCategory2)).thenReturn(java.util.List.of(mockSecondCategory2));

        // When
        ticketLogService.updateFirstCategory(2L, 1L, request1stCategory);

        // Then
        assertEquals(mockFirstCategory2, mockTicket.getFirstCategory());
    }

    @Test
    @DisplayName("담당자가 1차 카테고리 변경 실패 - 담당자 정보가 티켓과 일치하지 않음")
    void changeFirstCategoryFailInvalidManager() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(java.util.Optional.of(mockTicket));
        when(memberRepository.findById(3L)).thenReturn(java.util.Optional.of(mockManager2));

        // When & Then
        assertThatThrownBy(() ->
                ticketLogService.updateFirstCategory(3L, 1L, request1stCategory)
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.INVALID_TICKET_MANAGER));
    }

    @Test
    @DisplayName("담당자가 1차 카테고리 변경 실패 - 티켓이 존재하지 않음")
    void changeFirstCategoryFailTicketNotFound() {
        // When & Then
        assertThatThrownBy(() ->
                ticketLogService.updateFirstCategory(2L, 1L, request1stCategory)
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.TICKET_NOT_FOUND));
    }

    @Test
    @DisplayName("담당자가 1차 카테고리 변경 실패 - 담당자가 존재하지 않음")
    void changeFirstCategoryFailManagerNotFound() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(java.util.Optional.of(mockTicket));

        // When & Then
        assertThatThrownBy(() ->
                ticketLogService.updateFirstCategory(5L, 1L, request1stCategory)
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    @DisplayName("담당자가 1차 카테고리 변경 실패 - 1차 카테고리가 존재하지 않음")
    void changeFirstCategoryFailFirstCategoryNotFound() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(java.util.Optional.of(mockTicket));
        when(memberRepository.findById(2L)).thenReturn(java.util.Optional.of(mockManager));

        // When & Then
        assertThatThrownBy(() ->
                ticketLogService.updateFirstCategory(2L, 1L, request1stCategory)
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.CATEGORY_NOT_FOUND_FIRST));
    }

    @Test
    @DisplayName("담당자가 1차 카테고리 변경 실패 - 2차 카테고리가 존재하지 않음")
    void changeFirstCategoryFailSecondCategoryNotFound() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(java.util.Optional.of(mockTicket));
        when(memberRepository.findById(2L)).thenReturn(java.util.Optional.of(mockManager));
        when(categoryRepository.findByNameAndParentIsNull("ChangedCategory1")).thenReturn(java.util.Optional.of(mockFirstCategory2));
        when(categoryRepository.findByParentOrderByIdAsc(mockFirstCategory2)).thenReturn(java.util.List.of());

        // When & Then
        assertThatThrownBy(() ->
                ticketLogService.updateFirstCategory(2L, 1L, request1stCategory)
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.CATEGORY_NOT_FOUND_SECOND));
    }

    @Test
    @DisplayName("담당자가 2차 카테고리 변경 성공")
    void changeSecondCategorySuccess() {
        // Given
        Category duplicateSecCate = new Category(mockFirstCategory2, "DuplicateCategory", "C3", "Testing The Duplicate");
        setId(duplicateSecCate, 5L);

        when(ticketRepository.findById(2L)).thenReturn(java.util.Optional.of(mockTicket2));
        when(memberRepository.findById(2L)).thenReturn(java.util.Optional.of(mockManager));
        when(categoryRepository.findByNameAndParent("DuplicateCategory", mockFirstCategory2)).thenReturn(java.util.Optional.of(duplicateSecCate));

        // When
        ticketLogService.updateSecondCategory(2L, 2L, 3L, request2ndCategory);

        // Then
        assertEquals(duplicateSecCate, mockTicket2.getSecondCategory());
    }

    @Test
    @DisplayName("담당자가 2차 카테고리 변경 실패 - 담당자 정보가 티켓과 일치하지 않음")
    void changeSecondCategoryFailInvalidManager() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(java.util.Optional.of(mockTicket));
        when(memberRepository.findById(3L)).thenReturn(java.util.Optional.of(mockManager2));

        // When & Then
        assertThatThrownBy(() ->
                ticketLogService.updateSecondCategory(3L, 1L, 3L, request2ndCategory)
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.INVALID_TICKET_MANAGER));
    }

    @Test
    @DisplayName("담당자가 2차 카테고리 변경 실패 - 티켓이 존재하지 않음")
    void changeSecondCategoryFailTicketNotFound() {
        // When & Then
        assertThatThrownBy(() ->
                ticketLogService.updateSecondCategory(2L, 1L, 3L, request2ndCategory)
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.TICKET_NOT_FOUND));
    }

    @Test
    @DisplayName("담당자가 2차 카테고리 변경 실패 - 담당자가 존재하지 않음")
    void changeSecondCategoryFailManagerNotFound() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(java.util.Optional.of(mockTicket));

        // When & Then
        assertThatThrownBy(() ->
                ticketLogService.updateSecondCategory(5L, 1L, 3L, request2ndCategory)
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    @DisplayName("담당자가 2차 카테고리 변경 실패 - 1차 카테고리가 존재하지 않음")
    void changeSecondCategoryFailFirstCategoryNotFound() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(java.util.Optional.of(mockTicket));
        when(memberRepository.findById(2L)).thenReturn(java.util.Optional.of(mockManager));

        // When & Then
        assertThatThrownBy(() ->
                ticketLogService.updateSecondCategory(2L, 1L, 3L, request2ndCategory)
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.CATEGORY_NOT_FOUND_FIRST));
    }

    @Test
    @DisplayName("담당자가 2차 카테고리 변경 실패 - 2차 카테고리가 존재하지 않음")
    void changeSecondCategoryFailSecondCategoryNotFound() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(java.util.Optional.of(mockTicket));
        when(memberRepository.findById(2L)).thenReturn(java.util.Optional.of(mockManager));
        when(categoryRepository.findByNameAndParent("DuplicateCategory", mockFirstCategory)).thenReturn(java.util.Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                ticketLogService.updateSecondCategory(2L, 1L, 1L, request2ndCategory)
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.CATEGORY_NOT_FOUND_SECOND));
    }

    @Test
    @DisplayName("담당자 변경 성공")
    void reassignManagerSuccess() {
        // Given
        Ticket reassignTicket = Ticket.builder()
                .customId("0212D-I001")
                .user(mockUser)
                .firstCategory(mockFirstCategory)
                .secondCategory(mockSecondCategory)
                .title("Test Ticket")
                .content("This is a test ticket.")
                .priority(Priority.UNDEFINED)
                .status(Status.IN_PROGRESS)
                .dueDate(LocalDate.now().plusDays(1))
                .build();
        setId(reassignTicket, 2L);


        when(ticketRepository.findById(2L)).thenReturn(java.util.Optional.of(reassignTicket));
        when(memberRepository.findByUsername("testManager2")).thenReturn(java.util.Optional.of(mockManager2));

        // When
        ticketLogService.assignManager(3L, 2L, "testManager2");

        // Then
        assertEquals(mockManager2, reassignTicket.getManager());
    }

    @Test
    @DisplayName("담당자 변경 실패 - 티켓이 존재하지 않음")
    void reassignManagerFailTicketNotFound() {
        // When & Then
        assertThatThrownBy(() ->
                ticketLogService.assignManager(3L, 2L, "testManager2")
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.TICKET_NOT_FOUND));
    }

    @Test
    @DisplayName("담당자 변경 실패 - 담당자가 존재하지 않음")
    void reassignManagerFailManagerNotFound() {
        // Given
        Ticket reassignTicket = Ticket.builder()
                .customId("0212D-I001")
                .user(mockUser)
                .firstCategory(mockFirstCategory)
                .secondCategory(mockSecondCategory)
                .title("Test Ticket")
                .content("This is a test ticket.")
                .priority(Priority.UNDEFINED)
                .status(Status.IN_PROGRESS)
                .dueDate(LocalDate.now().plusDays(1))
                .build();
        setId(reassignTicket, 2L);

        when(ticketRepository.findById(2L)).thenReturn(java.util.Optional.of(reassignTicket));

        // When & Then
        assertThatThrownBy(() ->
                ticketLogService.assignManager(3L, 2L, "testManager2")
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    @DisplayName("담당자 변경 실패 - 담당자가 이미 할당됨")
    void reassignManagerFailManagerAlreadyAssigned() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(java.util.Optional.of(mockTicket));
        when(memberRepository.findByUsername("testManager2")).thenReturn(java.util.Optional.of(mockManager2));

        // When & Then
        assertThatThrownBy(() ->
                ticketLogService.assignManager(3L, 1L, "testManager2")
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.TICKET_ALREADY_ASSIGNED));
    }

    @Test
    @DisplayName("담당자 변경 실패 - 자기 자신을 담당자로 재할당")
    void reassignManagerFailAssignToSelf() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(java.util.Optional.of(mockTicket));
        when(memberRepository.findByUsername("testManager")).thenReturn(java.util.Optional.of(mockManager));

        // When & Then
        assertThatThrownBy(() ->
                ticketLogService.assignManager(2L, 1L, "testManager")
        ).isInstanceOf(ApiException.class)
                .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.TICKET_ALREADY_ASSIGNED_TO_SELF));
    }
}