package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.converter.TicketResponseConverter;
import com.quartz.checkin.dto.ticket.response.SoftDeletedTicketResponse;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.QMember;
import com.quartz.checkin.entity.QTicket;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.repository.TicketQueryRepository;
import com.quartz.checkin.repository.TicketRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketTrashServiceImpl implements TicketTrashService {
    private final MemberService memberService;
    private final TicketRepository ticketRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TicketQueryRepository ticketQueryRepository;
    private final JPAQueryFactory queryFactory;

    private static final QTicket ticket = QTicket.ticket;
    private static final QMember manager = new QMember("manager");


    @Transactional
    @Override
    public void restoreTickets(Long memberId, List<Long> ticketIds) {
        List<Ticket> tickets = findAndValidateTickets(memberId, ticketIds);

        for (Ticket ticket : tickets) {
            ticket.restoreTicket();
        }

        ticketRepository.saveAll(tickets);
    }

    @Transactional
    @Override
    public void deleteTickets(Long memberId, List<Long> ticketIds) {
        List<Ticket> tickets = findAndValidateTickets(memberId, ticketIds);

        ticketRepository.deleteAll(tickets);
    }

    // 티켓을 조회하고 사용자가 생성한 티켓인지 검증하는 공통 메서드
    private List<Ticket> findAndValidateTickets(Long memberId, List<Long> ticketIds) {
        // 현재 사용자 조회
        Member member = memberService.getMemberByIdOrThrow(memberId);

        // 요청된 티켓 ID 목록에 해당하는 티켓 조회
        List<Ticket> tickets = ticketRepository.findAllById(ticketIds);

        // 존재하지 않는 티켓이 있는 경우 예외 처리
        if (tickets.size() != ticketIds.size()) {
            throw new ApiException(ErrorCode.TICKET_NOT_FOUND);
        }

        // 사용자가 생성한 티켓인지 검증
        for (Ticket ticket : tickets) {
            if (!ticket.getUser().getId().equals(member.getId())) {
                throw new ApiException(ErrorCode.FORBIDDEN);
            }
        }

        return tickets; // 검증된 티켓 반환
    }

//    private Ticket replaceTicket(Ticket ticket, LocalDate dueDateTime) {
//        Ticket temp;
//
//        // ticket id를 가져와서 첫 네 글자를 오늘 날짜로 변경(MMDD)
//        String id = ticket.getCustomId();
//        String today = LocalDate.now().toString().substring(5).replace("-", "");
//        String tempCustomId = today + id.substring(4);
//
//        // temp에 ticket 깊은 복사
//        temp = Ticket.builder()
//                .customId(tempCustomId)
//                .user(ticket.getUser())
//                .firstCategory(ticket.getFirstCategory())
//                .secondCategory(ticket.getSecondCategory())
//                .title(ticket.getTitle())
//                .content(ticket.getContent())
//                .priority(ticket.getPriority())
//                .status(ticket.getStatus())
//                .dueDate(dueDateTime)
//                .agitId(ticket.getAgitId())
//                .build();
//
//    }

    @Transactional
    @Scheduled(cron = "0 0 3 * * ?") // 매일 새벽 3시 실행
    public void deleteExpiredTickets() {
        LocalDate today = LocalDate.now();
        LocalDate thresholdDate = today.minusDays(7);

        // QueryDSL을 활용하여 만료된 티켓 조회
        List<Ticket> expiredTickets = ticketQueryRepository.findTicketsToDelete(thresholdDate);

        if (!expiredTickets.isEmpty()) {
            // SoftDelete 수행
            expiredTickets.forEach(Ticket::softDelete);
            ticketRepository.saveAll(expiredTickets);
        }
    }

    // 매일 자정 실행 (자동 삭제)
    @Scheduled(cron = "0 0 0 * * ?")  // 매일 00:00:00에 실행
    @Transactional
    public void softDeleteOldClosedTickets() {
        // 6개월 전 날짜 계산
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);

        // 6개월 이상 지난 Closed 상태의 티켓 조회
        List<Ticket> oldTickets = queryFactory
                .selectFrom(ticket)
                .where(ticket.status.eq(Status.CLOSED)
                        .and(ticket.dueDate.before(sixMonthsAgo))
                        .and(ticket.deletedAt.isNull()))  // SoftDelete 되지 않은 것만 조회
                .fetch();

        if (!oldTickets.isEmpty()) {
            // SoftDelete 처리
            oldTickets.forEach(Ticket::softDelete);
            // 변경 사항 저장
            ticketRepository.saveAll(oldTickets);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public SoftDeletedTicketResponse getDeletedTickets(Long memberId, int page, int size) {
        Page<Ticket> ticketPage = fetchDeletedTickets(memberId, page, size);
        validatePagination(page, size, ticketPage.getTotalPages());
        return TicketResponseConverter.toSoftDeletedTicketResponse(ticketPage);
    }

    private Page<Ticket> fetchDeletedTickets(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);

        List<Ticket> ticketList = queryFactory
                .selectFrom(ticket)
                .leftJoin(ticket.manager, manager).fetchJoin()
                .where(ticket.deletedAt.isNotNull()
                        .and(ticket.user.id.eq(memberId)))
                .orderBy(ticket.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long totalCount = Optional.ofNullable(queryFactory
                .select(ticket.count())
                .from(ticket)
                .where(ticket.deletedAt.isNotNull()
                        .and(ticket.user.id.eq(memberId)))
                .fetchOne()).orElse(0L);
        return new PageImpl<>(ticketList, pageable, totalCount);
    }

    @Transactional
    @Scheduled(cron = "0 0 2 * * ?")
    public void deleteOldSoftDeletedTickets() {
        QTicket ticket = QTicket.ticket;
        // 30일이 지난 삭제된 티켓 조회
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<Ticket> expiredTickets = queryFactory
                .selectFrom(ticket)
                .where(ticket.deletedAt.isNotNull()
                        .and(ticket.deletedAt.before(thirtyDaysAgo)))
                .fetch();
        // 영구 삭제 실행
        ticketRepository.deleteAll(expiredTickets);
    }


    private void validatePagination(int page, int size, int totalPages) {
        if (page < 1) {
            throw new ApiException(ErrorCode.INVALID_PAGE_NUMBER);
        }
        if (size <= 0) {
            throw new ApiException(ErrorCode.INVALID_PAGE_SIZE);
        }
        if (page > totalPages && totalPages > 0) {
            throw new ApiException(ErrorCode.INVALID_PAGE_NUMBER);
        }
    }
}
