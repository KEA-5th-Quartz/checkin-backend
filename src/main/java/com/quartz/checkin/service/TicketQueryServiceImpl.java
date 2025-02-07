package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.converter.TicketResponseConverter;
import com.quartz.checkin.dto.ticket.response.ManagerTicketListResponse;
import com.quartz.checkin.dto.ticket.response.TicketDetailResponse;
import com.quartz.checkin.dto.ticket.response.TicketProgressResponse;
import com.quartz.checkin.dto.ticket.response.UserTicketListResponse;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Priority;
import com.quartz.checkin.entity.QMember;
import com.quartz.checkin.entity.QTicket;
import com.quartz.checkin.entity.Role;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.entity.TicketAttachment;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.repository.TicketAttachmentRepository;
import com.quartz.checkin.repository.TicketRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketQueryServiceImpl implements TicketQueryService {

    private final TicketRepository ticketRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final MemberRepository memberRepository;
    private final JPAQueryFactory queryFactory;


    @Override
    public TicketDetailResponse getTicketDetail(Long memberId, Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));

        boolean isManager = member.getRole() == Role.MANAGER;

        if (!isManager && !ticket.getUser().getId().equals(memberId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        List<TicketAttachment> attachments = ticketAttachmentRepository.findByTicketId(ticketId);

        return TicketDetailResponse.from(ticket, attachments);
    }

    @Override
    public ManagerTicketListResponse getManagerTickets(Long memberId, List<Status> statuses, List<String> usernames,
                                                       List<String> categories, List<Priority> priorities,
                                                       Boolean dueToday, Boolean dueThisWeek, int page, int size, String sortByCreatedAt) {

        Page<Ticket> ticketPage = fetchTickets(null, statuses, usernames, categories, priorities, dueToday, dueThisWeek, page, size, sortByCreatedAt);
        return TicketResponseConverter.toManagerTicketListResponse(ticketPage);
    }

    @Override
    public UserTicketListResponse getUserTickets(Long userId, List<Status> statuses, List<String> usernames,
                                                 List<String> categories, List<Priority> priorities,
                                                 Boolean dueToday, Boolean dueThisWeek, int page, int size, String sortByCreatedAt) {
        Page<Ticket> ticketPage = fetchTickets(userId, statuses, usernames, categories, priorities, dueToday, dueThisWeek, page, size, sortByCreatedAt);
        return TicketResponseConverter.toUserTicketListResponse(ticketPage);
    }

    @Override
    public ManagerTicketListResponse searchManagerTickets(Long memberId, String keyword, int page, int size, String sortByCreatedAt) {
        Page<Ticket> ticketPage = fetchSearchedTickets(null, keyword, page, size, sortByCreatedAt);
        return TicketResponseConverter.toManagerTicketListResponse(ticketPage);
    }

    @Override
    public UserTicketListResponse searchUserTickets(Long memberId, String keyword, int page, int size, String sortByCreatedAt) {
        Page<Ticket> ticketPage = fetchSearchedTickets(memberId, keyword, page, size, sortByCreatedAt);
        return TicketResponseConverter.toUserTicketListResponse(ticketPage);
    }

    @Transactional(readOnly = true)
    @Override
    public TicketProgressResponse getManagerProgress(Long managerId) {
        QTicket ticket = QTicket.ticket;

        LocalDate today = LocalDate.now();

        // dueTodayCount (오늘 마감이며 본인 담당 티켓)
        Long dueTodayCount = queryFactory
                .select(ticket.count())
                .from(ticket)
                .where(ticket.deletedAt.isNull()
                        .and(ticket.dueDate.eq(today))
                        .and(ticket.manager.id.eq(managerId)))
                .fetchOne();

        // openTicketCount (열린 상태이며 오늘 이후 마감)
        Long openTicketCount = queryFactory
                .select(ticket.count())
                .from(ticket)
                .where(ticket.deletedAt.isNull()
                        .and(ticket.status.eq(Status.OPEN))
                        .and(ticket.dueDate.goe(today)))
                .fetchOne();

        // inProgressTicketCount (진행 중 상태이며 본인 담당, 오늘 이후 마감)
        Long inProgressTicketCount = queryFactory
                .select(ticket.count())
                .from(ticket)
                .where(ticket.deletedAt.isNull()
                        .and(ticket.status.eq(Status.IN_PROGRESS))
                        .and(ticket.dueDate.goe(today))
                        .and(ticket.manager.id.eq(managerId)))
                .fetchOne();

        // closedTicketCount (완료 상태이며 본인 담당, 오늘 이후 마감)
        Long closedTicketCount = queryFactory
                .select(ticket.count())
                .from(ticket)
                .where(ticket.deletedAt.isNull()
                        .and(ticket.status.eq(Status.CLOSED))
                        .and(ticket.dueDate.goe(today))
                        .and(ticket.manager.id.eq(managerId)))
                .fetchOne();

        // totalTickets (모든 티켓에서 오늘 이후 마감된 티켓 수)
        Long totalTickets = queryFactory
                .select(ticket.count())
                .from(ticket)
                .where(ticket.deletedAt.isNull()
                        .and(ticket.dueDate.goe(today)))
                .fetchOne();

        // Null 값 방지
        int safeDueTodayCount = dueTodayCount != null ? dueTodayCount.intValue() : 0;
        int safeOpenTicketCount = openTicketCount != null ? openTicketCount.intValue() : 0;
        int safeInProgressTicketCount = inProgressTicketCount != null ? inProgressTicketCount.intValue() : 0;
        int safeClosedTicketCount = closedTicketCount != null ? closedTicketCount.intValue() : 0;
        int safeTotalTickets = totalTickets != null ? totalTickets.intValue() : 0;

        String progressExpression = safeTotalTickets > 0
                ? String.format("%d / %d", safeInProgressTicketCount + safeClosedTicketCount, safeTotalTickets)
                : "0 / 0";

        return new TicketProgressResponse(
                safeDueTodayCount,
                safeOpenTicketCount,
                safeInProgressTicketCount,
                safeClosedTicketCount,
                progressExpression
        );
    }


    private Page<Ticket> fetchSearchedTickets(Long memberId, String keyword, int page, int size, String sortByCreatedAt) {
        validatePagination(page, size);

        Sort.Direction createdAtSortDirection = "asc".equalsIgnoreCase(sortByCreatedAt) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(createdAtSortDirection, "createdAt");
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        QTicket ticket = QTicket.ticket;
        QMember manager = QMember.member;

        BooleanBuilder whereClause = new BooleanBuilder();
        whereClause.and(ticket.deletedAt.isNull());

        if (memberId != null) {
            whereClause.and(ticket.user.id.eq(memberId));
        }

        if (keyword != null && !keyword.isBlank()) {
            String searchKeyword = "%" + keyword.toLowerCase() + "%";
            whereClause.and(ticket.title.lower().like(searchKeyword)
                    .or(ticket.content.lower().like(searchKeyword)));
        }

        OrderSpecifier<?> orderSpecifier = "asc".equalsIgnoreCase(sortByCreatedAt)
                ? ticket.createdAt.asc()
                : ticket.createdAt.desc();

        // QueryDSL을 사용한 페이징 적용 조회
        List<Ticket> results = queryFactory
                .selectFrom(ticket)
                .leftJoin(ticket.manager, manager).fetchJoin()
                .where(whereClause)
                .orderBy(orderSpecifier)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 총 개수 조회
        long safeTotalCount = Optional.ofNullable(
                queryFactory.select(ticket.count())
                        .from(ticket)
                        .where(whereClause)
                        .fetchOne()
        ).orElse(0L);

        return new PageImpl<>(results, pageable, safeTotalCount);
    }

    private Page<Ticket> fetchTickets(Long memberId, List<Status> statuses, List<String> usernames,
                                      List<String> categories, List<Priority> priorities,
                                      Boolean dueToday, Boolean dueThisWeek, int page, int size, String sortByCreatedAt) {

        validatePagination(page, size);

        Sort.Direction createdAtSortDirection = "asc".equalsIgnoreCase(sortByCreatedAt) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(createdAtSortDirection, "createdAt");

        Pageable pageable = PageRequest.of(page - 1, size, sort);

        QTicket ticket = QTicket.ticket;
        QMember manager = QMember.member;

        LocalDate today = LocalDate.now();
        LocalDate endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        BooleanBuilder whereClause = new BooleanBuilder();
        whereClause.and(ticket.deletedAt.isNull());

        if (memberId != null) {
            whereClause.and(ticket.user.id.eq(memberId));
        }

        if (statuses != null && !statuses.isEmpty()) {
            whereClause.and(ticket.status.in(statuses));
        }

        if (categories != null && !categories.isEmpty()) {
            whereClause.and(ticket.firstCategory.name.lower().trim().in(categories));
        }

        if (priorities != null && !priorities.isEmpty()) {
            whereClause.and(ticket.priority.in(priorities));
        }

        if (usernames != null && !usernames.isEmpty()) {
            whereClause.and(ticket.manager.username.in(usernames));
        }

        if (Boolean.TRUE.equals(dueToday)) {
            whereClause.and(ticket.dueDate.eq(today));
        }

        if (Boolean.TRUE.equals(dueThisWeek)) {
            whereClause.and(ticket.dueDate.between(today, endOfWeek));
        }

        OrderSpecifier<?> orderSpecifier = "asc".equalsIgnoreCase(sortByCreatedAt)
                ? ticket.createdAt.asc()
                : ticket.createdAt.desc();

        // QueryDSL로 페이징 적용한 조회 쿼리 실행
        List<Ticket> results = queryFactory
                .selectFrom(ticket)
                .leftJoin(ticket.manager, manager).fetchJoin()
                .where(whereClause)
                .orderBy(orderSpecifier)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 총 개수 조회
        long safeTotalCount = Optional.ofNullable(
                queryFactory.select(ticket.count())
                        .from(ticket)
                        .where(whereClause)
                        .fetchOne()
        ).orElse(0L);

        return new PageImpl<>(results, pageable, safeTotalCount);
    }

    private void validatePagination(int page, int size) {
        if (page < 1) throw new ApiException(ErrorCode.INVALID_PAGE_NUMBER);
        if (size <= 0) throw new ApiException(ErrorCode.INVALID_PAGE_SIZE);
    }
}