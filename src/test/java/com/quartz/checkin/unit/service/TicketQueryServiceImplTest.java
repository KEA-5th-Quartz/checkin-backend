package com.quartz.checkin.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.ticket.response.ManagerTicketListResponse;
import com.quartz.checkin.dto.ticket.response.TicketDetailResponse;
import com.quartz.checkin.dto.ticket.response.TicketProgressResponse;
import com.quartz.checkin.dto.ticket.response.UserTicketListResponse;
import com.quartz.checkin.entity.*;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.repository.TicketAttachmentRepository;
import com.quartz.checkin.repository.TicketRepository;
import com.quartz.checkin.service.TicketQueryServiceImpl;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TicketQueryServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketAttachmentRepository ticketAttachmentRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private JPAQueryFactory queryFactory;

    @Mock
    private JPAQuery<Ticket> mockJPAQuery;

    @Mock
    private JPAQuery<Long> mockJPAQueryLong;

    @InjectMocks
    private TicketQueryServiceImpl ticketQueryService;

    private Member mockManager;
    private Member mockUser;
    private Ticket mockTicket;

    @BeforeEach
    void setUp() {
        mockManager = Member.builder()
                .id(2L)
                .username("manager")
                .email("manager@example.com")
                .role(Role.MANAGER)
                .build();

        mockUser = Member.builder()
                .id(1L)
                .username("testUser")
                .email("user@example.com")
                .role(Role.USER)
                .build();

        Category firstCategory = new Category(null, "1st", "1st", "Development and Operations Guide");
        Category secondCategory = new Category(firstCategory, "2nd", "2nd", "Infrastructure Management Guide");

        mockTicket = Ticket.builder()
                .customId("0212INFR-CRT001")
                .user(mockUser)
                .firstCategory(firstCategory)
                .secondCategory(secondCategory)
                .title("Test Ticket")
                .content("This is a test ticket.")
                .priority(Priority.HIGH)
                .status(Status.IN_PROGRESS)
                .dueDate(LocalDate.now().plusDays(3))
                .build();

        Page<Ticket> mockPage = new PageImpl<>(List.of(mockTicket, mockTicket), PageRequest.of(0, 10), 2);

        lenient().when(ticketRepository.findAll(any(Pageable.class))).thenReturn(mockPage);
        lenient().when(ticketRepository.fetchTickets(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class), any()))
                .thenReturn(mockPage);
        lenient().when(ticketRepository.fetchSearchedTickets(any(), any(), any(Pageable.class), any()))
                .thenReturn(mockPage);
        lenient().when(ticketRepository.findById(10L)).thenReturn(Optional.of(mockTicket));
        lenient().when(memberRepository.findById(2L)).thenReturn(Optional.of(mockManager));

        QTicket qTicket = QTicket.ticket;
        lenient().when(queryFactory.select(qTicket.count())).thenReturn(mockJPAQueryLong);
        lenient().when(mockJPAQueryLong.from(qTicket)).thenReturn(mockJPAQueryLong);
        lenient().when(mockJPAQueryLong.fetchOne()).thenReturn(10L);

        lenient().when(ticketRepository.getManagerProgress(anyLong()))
                .thenReturn(new TicketProgressResponse(5L, 30L, 10L, 20L, "30 / 60"));
    }

    @Test
    @DisplayName("담당자 티켓 상세 조회 성공")
    void getTicketDetailSuccessForManager() {
        TicketDetailResponse response = ticketQueryService.getTicketDetail(2L, 10L);

        assertNotNull(response);
        assertEquals("Test Ticket", response.getTitle());
        assertEquals(Status.IN_PROGRESS, Status.valueOf(response.getStatus()));
    }

    @Test
    @DisplayName("타사용자 티켓 조회 제한")
    void getTicketDetailFailForbiddenForUser() {
        when(memberRepository.findById(3L)).thenReturn(Optional.of(
                Member.builder()
                        .id(3L)
                        .username("unauthorized")
                        .role(Role.USER)
                        .build()
        ));

        ApiException thrown = assertThrows(ApiException.class, () -> ticketQueryService.getTicketDetail(3L, 10L));
        assertEquals(ErrorCode.FORBIDDEN, thrown.getErrorCode());
    }

    @Test
    @DisplayName("담당자 티켓 목록 조회 성공")
    void getManagerTicketsSuccess() {
        ManagerTicketListResponse response = ticketQueryService.getManagerTickets(
                2L, List.of(Status.IN_PROGRESS), List.of("testUser"),
                List.of("DevOps"), List.of(Priority.HIGH), false, false, 1, 20, "desc");

        assertNotNull(response);
        assertFalse(response.getTickets().isEmpty());
    }

    @Test
    @DisplayName("사용자 본인 티켓 조회 성공")
    void getUserTicketsSuccess() {
        UserTicketListResponse response = ticketQueryService.getUserTickets(
                1L, List.of(Status.IN_PROGRESS), List.of("testUser"),
                List.of("DevOps"), List.of(Priority.HIGH), false, false, 1, 20, "desc");

        assertNotNull(response);
        assertFalse(response.getTickets().isEmpty());
    }

    @Test
    @DisplayName("담당자 티켓 검색 성공")
    void searchManagerTicketsSuccess() {
        ManagerTicketListResponse response = ticketQueryService.searchManagerTickets(2L, "Test", 1, 20, "desc");

        assertNotNull(response);
        assertFalse(response.getTickets().isEmpty());
    }

    @Test
    @DisplayName("사용자 티켓 검색 성공")
    void searchUserTicketsSuccess() {
        UserTicketListResponse response = ticketQueryService.searchUserTickets(1L, "Test", 1, 20, "desc");

        assertNotNull(response);
        assertFalse(response.getTickets().isEmpty());
    }

    @Test
    @DisplayName("담당자 티켓 진행률 조회 성공")
    void getManagerProgressSuccess() {
        TicketProgressResponse response = ticketQueryService.getManagerProgress(2L);

        assertNotNull(response);
        assertEquals("30 / 60", response.getProgressExpression());

        verify(ticketRepository, times(1)).getManagerProgress(2L);
    }
}
