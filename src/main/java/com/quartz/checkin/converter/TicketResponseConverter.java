package com.quartz.checkin.converter;

import com.quartz.checkin.dto.ticket.response.DeletedTicketDetailResponse;
import com.quartz.checkin.dto.ticket.response.ManagerTicketListResponse;
import com.quartz.checkin.dto.ticket.response.ManagerTicketSummaryResponse;
import com.quartz.checkin.dto.ticket.response.SoftDeletedTicketResponse;
import com.quartz.checkin.dto.ticket.response.UserTicketListResponse;
import com.quartz.checkin.dto.ticket.response.UserTicketSummaryResponse;
import com.quartz.checkin.entity.Ticket;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.Page;

public class TicketResponseConverter {

    public static ManagerTicketListResponse toManagerTicketListResponse(Page<Ticket> ticketPage) {
        List<ManagerTicketSummaryResponse> ticketList = ticketPage.hasContent()
                ? ticketPage.getContent().stream().map(ManagerTicketSummaryResponse::from).toList()
                : Collections.emptyList();

        return new ManagerTicketListResponse(
                getPageNumber(ticketPage),
                ticketPage.getSize(),
                ticketPage.getTotalPages(),
                ticketPage.getTotalElements(),
                ticketList
        );
    }

    public static UserTicketListResponse toUserTicketListResponse(Page<Ticket> ticketPage) {
        List<UserTicketSummaryResponse> ticketList = ticketPage.hasContent()
                ? ticketPage.getContent().stream().map(UserTicketSummaryResponse::from).toList()
                : Collections.emptyList();

        return new UserTicketListResponse(
                getPageNumber(ticketPage),
                ticketPage.getSize(),
                ticketPage.getTotalPages(),
                ticketPage.getTotalElements(),
                ticketList
        );
    }

    public static SoftDeletedTicketResponse toSoftDeletedTicketResponse(Page<Ticket> ticketPage) {
        List<DeletedTicketDetailResponse> ticketList = ticketPage.hasContent()
                ? ticketPage.getContent().stream().map(DeletedTicketDetailResponse::from).toList()
                : Collections.emptyList();

        return new SoftDeletedTicketResponse(
                getPageNumber(ticketPage),
                ticketPage.getSize(),
                ticketPage.getTotalPages(),
                ticketPage.getTotalElements(),
                ticketList
        );
    }


    private static int getPageNumber(Page<Ticket> ticketPage) {
        return ticketPage.getNumber() + 1;
    }
}