package com.quartz.checkin.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketSummaryResponse {
    private Long ticketId;
    private String title;
    private String firstCategory;
    private String secondCategory;
    private String manager;
    private String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd")
    private LocalDate dueDate;

    private Status status;

    public static TicketSummaryResponse from(Ticket ticket) {
        return TicketSummaryResponse.builder()
                .ticketId(ticket.getId())
                .title(ticket.getTitle())
                .firstCategory(ticket.getFirstCategory().getName())
                .secondCategory(ticket.getSecondCategory().getName())
                .manager(ticket.getManager() != null ? ticket.getManager().getUsername() : null)
                .content(ticket.getContent())
                .dueDate(ticket.getDueDate())
                .status(ticket.getStatus())
                .build();
    }
}
