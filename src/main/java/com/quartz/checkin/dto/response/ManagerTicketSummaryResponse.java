package com.quartz.checkin.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.quartz.checkin.entity.Priority;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagerTicketSummaryResponse {
    private Long ticketId;
    private String title;
    private String firstCategory;
    private String secondCategory;
    private String manager;
    private String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd")
    private LocalDate dueDate;

    private Priority priority;
    private Status status;

    public static ManagerTicketSummaryResponse from(Ticket ticket) {
        return ManagerTicketSummaryResponse.builder()
                .ticketId(ticket.getId())
                .title(ticket.getTitle())
                .firstCategory(ticket.getFirstCategory().getName())
                .secondCategory(ticket.getSecondCategory().getName())
                .manager(ticket.getManager() != null ? ticket.getManager().getUsername() : null)
                .content(ticket.getContent())
                .dueDate(ticket.getDueDate())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .build();
    }
}
