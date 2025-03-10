package com.quartz.checkin.dto.ticket.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.entity.TicketAttachment;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketDetailResponse {

    private Long ticketId;
    private String customId;
    private String title;
    private String firstCategory;
    private String secondCategory;
    private String username;
    private String managerProfilePic;
    private String manager;
    private String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd")
    private LocalDate dueDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime createdAt;

    private String priority;
    private String status;
    private List<String> ticketAttachmentUrls;

    public static TicketDetailResponse from(Ticket ticket, List<TicketAttachment> ticketAttachments) {
        List<String> attachmentUrls = ticketAttachments.stream()
                .map(ta -> ta.getAttachment().getUrl())
                .toList();

        return TicketDetailResponse.builder()
                .ticketId(ticket.getId())
                .customId(ticket.getCustomId())
                .title(ticket.getTitle())
                .firstCategory(ticket.getFirstCategory().getName())
                .secondCategory(ticket.getSecondCategory().getName())
                .username(ticket.getUser().getUsername())
                .manager(ticket.getManager() != null ? ticket.getManager().getUsername() : null)
                .managerProfilePic(ticket.getManager() != null ? ticket.getManager().getProfilePic() : null)
                .content(ticket.getContent())
                .dueDate(ticket.getDueDate())
                .createdAt(ticket.getCreatedAt())
                .priority(ticket.getPriority() != null ? ticket.getPriority().name() : null)
                .status(ticket.getStatus().name())
                .ticketAttachmentUrls(attachmentUrls)
                .build();
    }
}
