package com.quartz.checkin.dto.ticket.response;

import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeletedTicketDetailResponse {
    private Long ticketId;
    private String customId;
    private String title;
    private String manager;
    private String managerProfilePic;
    private String content;
    private LocalDate dueDate;
    private Status status;

    public static DeletedTicketDetailResponse from(Ticket ticket) {
        return DeletedTicketDetailResponse.builder()
                .ticketId(ticket.getId())
                .customId(ticket.getCustomId())
                .title(ticket.getTitle())
                .manager(ticket.getManager() != null ? ticket.getManager().getUsername() : null)
                .managerProfilePic(ticket.getManager() != null ? ticket.getManager().getProfilePic() : null)
                .content(ticket.getContent())
                .dueDate(ticket.getDueDate())
                .status(ticket.getStatus())
                .build();
    }
}
