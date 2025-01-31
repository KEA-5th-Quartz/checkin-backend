package com.quartz.checkin.converter;

import com.quartz.checkin.dto.ticket.response.ManagerTicketListResponse;
import com.quartz.checkin.dto.ticket.response.ManagerTicketSummaryResponse;
import com.quartz.checkin.dto.ticket.response.UserTicketListResponse;
import com.quartz.checkin.dto.ticket.response.UserTicketSummaryResponse;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;

public class TicketResponseConverter {

    public static ManagerTicketListResponse toManagerTicketListResponse(Page<Ticket> ticketPage, List<Ticket> allTickets, int totalTickets, int openTicketCount) {
        List<ManagerTicketSummaryResponse> ticketList = ticketPage.getContent().stream()
                .map(ManagerTicketSummaryResponse::from)
                .toList();

        // 오늘까지 마감된 티켓 수
        int dueTodayCount = (int) allTickets.stream()
                .filter(ticket -> !ticket.getDueDate().isAfter(LocalDate.now()))
                .count();

        // 나에게 할당된 진행 중(IN_PROGRESS) 티켓 수
        int inProgressTicketCount = (int) allTickets.stream()
                .filter(ticket -> ticket.getStatus() == Status.IN_PROGRESS)
                .count();

        // 나에게 할당된 완료된(CLOSED) 티켓 수
        int closedTicketCount = (int) allTickets.stream()
                .filter(ticket -> ticket.getStatus() == Status.CLOSED)
                .count();

        String progressExpression = totalTickets == 0 ? "0 / 0" : (inProgressTicketCount + closedTicketCount) + " / " + totalTickets;

        return new ManagerTicketListResponse(
                ticketPage.getNumber() + 1,
                ticketPage.getSize(),
                ticketPage.getTotalPages(),
                (int) ticketPage.getTotalElements(),
                dueTodayCount,
                openTicketCount,
                inProgressTicketCount,
                closedTicketCount,
                progressExpression,
                ticketList
        );
    }

    public static UserTicketListResponse toUserTicketListResponse(Page<Ticket> ticketPage) {
        List<UserTicketSummaryResponse> ticketList = ticketPage.getContent().stream()
                .map(UserTicketSummaryResponse::from)
                .toList();

        return new UserTicketListResponse(
                ticketPage.getNumber() + 1,
                ticketPage.getSize(),
                ticketPage.getTotalPages(),
                (int) ticketPage.getTotalElements(),
                ticketList
        );
    }
}
