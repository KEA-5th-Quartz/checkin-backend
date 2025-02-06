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
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TicketDeleteServiceImpl implements TicketDeleteService {
    private final MemberService memberService;
    private final TicketRepository ticketRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final AttachmentService attachmentService;

    @Override
    public void restoreTickets(Long memberId, List<String> ticketIds) {
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

        ArrayList<Ticket> temps = new ArrayList<>();
        // 티켓 복원 시 dueDate와 createdAt을 오늘을 기준으로 초기화
        LocalDateTime now = LocalDateTime.now();
        tickets.forEach(ticket -> {
            Duration duration = Duration.between(ticket.getCreatedAt(), now);
            LocalDateTime dueDateTime = ticket.getDueDate().atStartOfDay().plus(duration);
            Ticket temp = replaceTicket(ticket, dueDateTime.toLocalDate());
            temps.add(temp);
        });
        ticketRepository.saveAll(temps);
    }

    @Override
    public void purgeTickets(Long memberId, List<String> ticketIds) {
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

        // 티켓에 연결된 첨부파일 ID 조회
        List<Long> attachmentIds = ticketAttachmentRepository.findAttachmentIdsByTicketIds(ticketIds);

        // 첨부파일 영구 삭제
        if (!attachmentIds.isEmpty()) {
            ticketAttachmentRepository.deleteAllByTicketIds(ticketIds); // 연결 데이터 삭제
            attachmentService.deleteAttachments(attachmentIds); // 첨부파일 삭제
        }

        // 티켓 영구 삭제
        ticketRepository.deleteAll(tickets);
    }

    private Ticket replaceTicket(Ticket ticket, LocalDate dueDateTime) {
        Ticket temp;

        // ticket id를 가져와서 첫 네 글자를 오늘 날짜로 변경(MMDD)
        String id = ticket.getId();
        String today = LocalDate.now().toString().substring(5).replace("-", "");
        String tempId = today + id.substring(4);

        // temp에 ticket 깊은 복사
        temp = Ticket.builder()
                .id(tempId)
                .user(ticket.getUser())
                .firstCategory(ticket.getFirstCategory())
                .secondCategory(ticket.getSecondCategory())
                .title(ticket.getTitle())
                .content(ticket.getContent())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .dueDate(dueDateTime)
                .agitId(ticket.getAgitId())
                .build();

        // ticket 삭제
        ticketRepository.delete(ticket);
        return temp;
    }
}
