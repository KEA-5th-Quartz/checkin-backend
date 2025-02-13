package com.quartz.checkin.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.ticket.request.TicketCreateRequest;
import com.quartz.checkin.dto.ticket.response.TicketCreateResponse;
import com.quartz.checkin.entity.*;
import com.quartz.checkin.repository.TicketRepository;
import com.quartz.checkin.repository.AttachmentRepository;
import com.quartz.checkin.repository.TicketAttachmentRepository;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;

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
    private Category firstCategory;
    private Category secondCategory;
    private Ticket mockTicket;

    @BeforeEach
    void setUp() {
        mockMember = Member.builder()
                .id(1L)
                .username("testUser")
                .email("test@example.com")
                .password("securePassword")
                .role(Role.USER)
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

    @Test
    void createTicket_Success() {
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
    void createTicket_Fail_NoTitle() {
        TicketCreateRequest request = new TicketCreateRequest(
                null, "This is a test ticket.", "DevOps", "Infrastructure",
                LocalDate.now().plusDays(1), List.of()
        );

        ApiException thrown = assertThrows(ApiException.class, () -> ticketCudService.createTicket(1L, request));
        assertEquals(ErrorCode.INVALID_DATA, thrown.getErrorCode());
    }

    @Test
    void createTicket_Fail_NoContent() {
        TicketCreateRequest request = new TicketCreateRequest(
                "Test Ticket", null, "DevOps", "Infrastructure",
                LocalDate.now().plusDays(1), List.of()
        );

        ApiException thrown = assertThrows(ApiException.class, () -> ticketCudService.createTicket(1L, request));
        assertEquals(ErrorCode.INVALID_DATA, thrown.getErrorCode());
    }

    @Test
    void createTicket_Fail_NoFirstCategory() {
        TicketCreateRequest request = new TicketCreateRequest(
                "Test Ticket", "This is a test ticket.", "",
                "Infrastructure", LocalDate.now().plusDays(1), List.of()
        );

        when(categoryService.getFirstCategoryOrThrow(anyString()))
                .thenThrow(new ApiException(ErrorCode.INVALID_TICKET_ID_FORMAT));

        ApiException thrown = assertThrows(ApiException.class, () -> ticketCudService.createTicket(1L, request));
        assertEquals(ErrorCode.INVALID_TICKET_ID_FORMAT, thrown.getErrorCode());
    }

    @Test
    void createTicket_Fail_NoSecondCategory() {
        TicketCreateRequest request = new TicketCreateRequest(
                "Test Ticket", "This is a test ticket.", "DevOps",
                "", LocalDate.now().plusDays(1), List.of()
        );

        when(categoryService.getSecondCategoryOrThrow(anyString(), any()))
                .thenThrow(new ApiException(ErrorCode.INVALID_TICKET_ID_FORMAT));

        ApiException thrown = assertThrows(ApiException.class, () -> ticketCudService.createTicket(1L, request));
        assertEquals(ErrorCode.INVALID_TICKET_ID_FORMAT, thrown.getErrorCode());
    }

    @Test
    void createTicket_Fail_InvalidDueDate_PastDate() {
        TicketCreateRequest request = new TicketCreateRequest(
                "Test Ticket", "This is a test ticket.", "DevOps", "Infrastructure",
                LocalDate.now().minusDays(1), List.of()
        );

        ApiException thrown = assertThrows(ApiException.class, () -> ticketCudService.createTicket(1L, request));
        assertEquals(ErrorCode.INVALID_DATA, thrown.getErrorCode());
    }

    @Test
    void createTicket_Fail_InvalidDueDate_Null() {
        TicketCreateRequest request = new TicketCreateRequest(
                "Test Ticket", "This is a test ticket.", "DevOps", "Infrastructure",
                null, List.of()
        );

        ApiException thrown = assertThrows(ApiException.class, () -> ticketCudService.createTicket(1L, request));
        assertEquals(ErrorCode.INVALID_DATA, thrown.getErrorCode());
    }
}