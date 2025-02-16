package com.quartz.checkin.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.ticket.request.PriorityUpdateRequest;
import com.quartz.checkin.dto.ticket.request.TicketCreateRequest;
import com.quartz.checkin.dto.ticket.response.TicketCreateResponse;
import com.quartz.checkin.entity.Category;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Priority;
import com.quartz.checkin.entity.Role;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.repository.AttachmentRepository;
import com.quartz.checkin.repository.TicketAttachmentRepository;
import com.quartz.checkin.repository.TicketRepository;
import com.quartz.checkin.service.CategoryServiceImpl;
import com.quartz.checkin.service.MemberService;
import com.quartz.checkin.service.TicketCudServiceImpl;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TicketCudServiceImplTest {

    @Mock
    private MemberService memberService;

    @Mock
    private CategoryServiceImpl categoryService;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private TicketAttachmentRepository ticketAttachmentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private JPAQueryFactory queryFactory;

    @Mock
    private JPAQuery<Object> mockJPAQuery;

    @InjectMocks
    private TicketCudServiceImpl ticketCudService;

    private Member mockMember;
    private Member mockManager;
    private Category firstCategory;
    private Category secondCategory;
    private Ticket mockTicket;
    private PriorityUpdateRequest requestPriorityUpdate;

    @BeforeEach
    void setUp() {
        mockMember = Member.builder()
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

        firstCategory = new Category(null, "DevOps", "D", "Development and Operations Guide");
        secondCategory = new Category(firstCategory, "Infrastructure", "I", "Infrastructure Management Guide");

        mockTicket = Ticket.builder()
                .customId("0212D-I001")
                .user(mockMember)
                .firstCategory(firstCategory)
                .secondCategory(secondCategory)
                .title("Test Ticket")
                .content("This is a test ticket.")
                .priority(Priority.UNDEFINED)
                .status(Status.OPEN)
                .dueDate(LocalDate.now().plusDays(1))
                .build();


        requestPriorityUpdate = new PriorityUpdateRequest();
        setPriority(requestPriorityUpdate, Priority.HIGH);

        mockJPAQuery = mock(JPAQuery.class);
        lenient().when(queryFactory.select(any(Expression.class))).thenReturn(mockJPAQuery);
        lenient().when(mockJPAQuery.from(any(EntityPathBase.class))).thenReturn(mockJPAQuery);
        lenient().when(mockJPAQuery.where(any(Predicate.class))).thenReturn(mockJPAQuery);
        lenient().when(mockJPAQuery.orderBy((OrderSpecifier<?>) any())).thenReturn(mockJPAQuery);
        lenient().when(mockJPAQuery.limit(anyLong())).thenReturn(mockJPAQuery);
        lenient().when(mockJPAQuery.fetchOne()).thenReturn("0212D-I099");

        lenient().when(categoryService.getFirstCategoryOrThrow(anyString())).thenReturn(firstCategory);
        lenient().when(categoryService.getSecondCategoryOrThrow(anyString(), any())).thenReturn(secondCategory);
        lenient().when(memberService.getMemberByIdOrThrow(1L)).thenReturn(mockMember);
    }

    private void setPriority(Object entity, Priority priority) {
        try {
            var field = entity.getClass().getDeclaredField("priority");
            field.setAccessible(true);
            field.set(entity, priority);
        }  catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("티켓 생성 성공")
    void createTicketSuccess() {
        TicketCreateRequest request = new TicketCreateRequest(
                "Test Ticket", "This is a test ticket.", "DevOps", "Infrastructure",
                LocalDate.now().plusDays(1), List.of()
        );

        when(ticketRepository.save(any(Ticket.class))).thenReturn(mockTicket);

        TicketCreateResponse response = ticketCudService.createTicket(1L, request);

        assertNotNull(response);
        assertEquals(mockTicket.getId(), response.ticketId());
    }

    @Test
    @DisplayName("티켓 생성 실패 - 첫 번째 카테고리 없음")
    void createTicketFailNoFirstCategory() {
        TicketCreateRequest request = new TicketCreateRequest(
                "Test Ticket", "This is a test ticket.", "",
                "Infrastructure", LocalDate.now().plusDays(1), List.of()
        );

        when(categoryService.getFirstCategoryOrThrow(anyString()))
                .thenThrow(new ApiException(ErrorCode.DUPLICATE_TICKET_ID));

        ApiException thrown = assertThrows(ApiException.class, () -> ticketCudService.createTicket(1L, request));
        assertEquals(ErrorCode.DUPLICATE_TICKET_ID, thrown.getErrorCode());
    }

    @Test
    @DisplayName("티켓 생성 실패 - 두 번째 카테고리 없음")
    void createTicketFailNoSecondCategory() {
        TicketCreateRequest request = new TicketCreateRequest(
                "Test Ticket", "This is a test ticket.", "DevOps",
                "", LocalDate.now().plusDays(1), List.of()
        );

        when(categoryService.getSecondCategoryOrThrow(anyString(), any()))
                .thenThrow(new ApiException(ErrorCode.DUPLICATE_TICKET_ID));

        ApiException thrown = assertThrows(ApiException.class, () -> ticketCudService.createTicket(1L, request));
        assertEquals(ErrorCode.DUPLICATE_TICKET_ID, thrown.getErrorCode());
    }

    @Test
    @DisplayName("티켓 중요도 변경 성공")
    void updateTicketPrioritySuccess() {
        // Given
        mockTicket.assignManager(mockManager);
        when(ticketRepository.findById(1L)).thenReturn(java.util.Optional.of(mockTicket));
        when(memberService.getMemberByIdOrThrow(2L)).thenReturn(mockManager);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(mockTicket);

        // When
        ticketCudService.updatePriority(2L, 1L, requestPriorityUpdate);

        // Then
        assertEquals(Priority.HIGH, mockTicket.getPriority());
    }

    @Test
    @DisplayName("티켓 중요도 변경 실패 - 담당자 없음")
    void updateTicketPriorityFailManagerNotFound() {
        // Given
        when(memberService.getMemberByIdOrThrow(2L)).thenThrow(new ApiException(ErrorCode.MEMBER_NOT_FOUND));

        // When
        ApiException thrown = assertThrows(ApiException.class, () -> ticketCudService.updatePriority(2L, 1L, requestPriorityUpdate));

        // Then
        assertEquals(ErrorCode.MEMBER_NOT_FOUND, thrown.getErrorCode());
    }

    @Test
    @DisplayName("티켓 중요도 변경 실패 - 티켓 없음")
    void updateTicketPriorityFailTicketNotFound() {
        // Given
        when(ticketRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        // When
        ApiException thrown = assertThrows(ApiException.class, () -> ticketCudService.updatePriority(2L, 1L, requestPriorityUpdate));

        // Then
        assertEquals(ErrorCode.TICKET_NOT_FOUND, thrown.getErrorCode());
    }

    @Test
    @DisplayName("티켓 중요도 변경 실패 - 담당자 불일치")
    void updateTicketPriorityFailManagerMismatch() {
        // Given
        mockTicket.assignManager(mockMember);
        when(ticketRepository.findById(1L)).thenReturn(java.util.Optional.of(mockTicket));
        when(memberService.getMemberByIdOrThrow(2L)).thenReturn(mockManager);

        // When
        ApiException thrown = assertThrows(ApiException.class, () -> ticketCudService.updatePriority(2L, 1L, requestPriorityUpdate));

        // Then
        assertEquals(ErrorCode.INVALID_TICKET_MANAGER, thrown.getErrorCode());
    }
}