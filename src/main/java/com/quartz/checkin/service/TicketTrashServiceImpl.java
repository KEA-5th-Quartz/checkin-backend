package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.repository.TicketAttachmentRepository;
import com.quartz.checkin.repository.TicketRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketTrashServiceImpl implements TicketTrashService {
    private final MemberService memberService;
    private final TicketRepository ticketRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final AttachmentService attachmentService;

    @Transactional
    @Override
    public void restoreTickets(Long memberId, List<Long> ticketIds) {
        // 현재 사용자 조회
        Member member = memberService.getMemberByIdOrThrow(memberId);

        // 복원할 티켓 조회
        List<Ticket> tickets = ticketRepository.findAllById(ticketIds);

        // 존재하지 않는 티켓이 있는 경우 예외 처리
        if (tickets.size() != ticketIds.size()) {
            throw new ApiException(ErrorCode.TICKET_NOT_FOUND);
        }

        // 사용자가 생성한 티켓인지 확인
        for (Ticket ticket : tickets) {
            if (!ticket.getUser().getId().equals(member.getId())) {
                throw new ApiException(ErrorCode.UNAUTHENTICATED);
            }
        }

//        ArrayList<Ticket> temps = new ArrayList<>();
        //            LocalDateTime now = LocalDateTime.now();
        //            // 티켓 복원 시 dueDate를 createdAt와의 차이만큼 더함
        //            LocalDateTime dueDateTime = ticket.getDueDate().atStartOfDay();
        //            Duration duration = Duration.between(ticket.getCreatedAt(), dueDateTime);
        //            ticket.updateDueDate(dueDateTime.plus(duration).toLocalDate());
        ////            Ticket temp = replaceTicket(ticket, dueDateTime.toLocalDate());
        ////            temps.add(temp);
        //            // ticket id를 가져와서 첫 네 글자를 오늘 날짜로 변경(MMDD)
        //            String id = ticket.getCustomId();
        //            String today = LocalDate.now().toString().substring(5).replace("-", "");
        //            ticket.updateCustomId(today + id.substring(4));
        tickets.forEach(this::restoreTicket);
        ticketRepository.saveAll(tickets);
    }

    @Transactional
    @Override
    public void deleteTickets(Long memberId, List<Long> ticketIds) {
        // 현재 사용자 조회
        Member member = memberService.getMemberByIdOrThrow(memberId);

        // 삭제할 티켓 조회
        List<Ticket> tickets = ticketRepository.findAllById(ticketIds);

        // 존재하지 않는 티켓이 있는 경우 예외 처리
        if (tickets.size() != ticketIds.size()) {
            throw new ApiException(ErrorCode.TICKET_NOT_FOUND);
        }

        // 사용자가 생성한 티켓인지 확인
        for (Ticket ticket : tickets) {
            if (!ticket.getUser().getId().equals(member.getId())) {
                throw new ApiException(ErrorCode.UNAUTHENTICATED);
            }
        }

        // 티켓 영구 삭제
        ticketRepository.deleteAll(tickets);
    }

    private void restoreTicket(Ticket ticket) {
        // 티켓 복원 시 dueDate를 createdAt와의 차이만큼 더함
        LocalDateTime dueDateTime = ticket.getDueDate().atStartOfDay();
        Duration duration = Duration.between(ticket.getCreatedAt(), dueDateTime);
        ticket.updateDueDate(dueDateTime.plus(duration).toLocalDate());
//            Ticket temp = replaceTicket(ticket, dueDateTime.toLocalDate());
//            temps.add(temp);

        // ticket id를 가져와서 첫 네 글자를 오늘 날짜로 변경(MMDD)
        String id = ticket.getCustomId();
        String today = LocalDate.now().toString().substring(5).replace("-", "");
        ticket.updateCustomId(today + id.substring(4));

        ticket.restoreTicket();
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
            log.info("Soft deleting {} expired tickets.", expiredTickets.size());

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
            log.info("Soft deleting {} closed tickets older than 6 months.", oldTickets.size());

            // SoftDelete 처리
            oldTickets.forEach(Ticket::softDelete);

            // 변경 사항 저장
            ticketRepository.saveAll(oldTickets);
        }
    }

}
