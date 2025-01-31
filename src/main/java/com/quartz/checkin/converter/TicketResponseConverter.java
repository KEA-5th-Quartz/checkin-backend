package com.quartz.checkin.converter;

import com.quartz.checkin.dto.response.ManagerTicketListResponse;
import com.quartz.checkin.dto.response.ManagerTicketSummaryResponse;
import com.quartz.checkin.dto.response.UserTicketListResponse;
import com.quartz.checkin.dto.response.UserTicketSummaryResponse;
import com.quartz.checkin.entity.Ticket;
import org.springframework.data.domain.Page;

import java.util.List;

public class TicketResponseConverter {

    public static ManagerTicketListResponse toManagerTicketListResponse(Page<Ticket> ticketPage) {
        List<ManagerTicketSummaryResponse> ticketList = ticketPage.getContent().stream()
                .map(ManagerTicketSummaryResponse::from)
                .toList();

        return new ManagerTicketListResponse(
                ticketPage.getNumber() + 1,
                ticketPage.getSize(),
                ticketPage.getTotalPages(),
                (int) ticketPage.getTotalElements(),
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
