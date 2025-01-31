package com.quartz.checkin.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import lombok.*;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTicketSummaryResponse {
    private Long ticketId;
    private String title;
    private String firstCategory;
    private String secondCategory;
    private String manager;
    private String managerProfliePic;
    private String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd")
    private LocalDate dueDate;

    private Status status;

    public static UserTicketSummaryResponse from(Ticket ticket) {
        return UserTicketSummaryResponse.builder()
                .ticketId(ticket.getId())
                .title(ticket.getTitle())
                .firstCategory(ticket.getFirstCategory().getName())
                .secondCategory(ticket.getSecondCategory().getName())
                .manager(ticket.getManager() != null ? ticket.getManager().getUsername() : null)
                .managerProfliePic(ticket.getManager().getProfilePic())
                .content(ticket.getContent())
                .dueDate(ticket.getDueDate())
                .status(ticket.getStatus())
                .build();
    }
}

