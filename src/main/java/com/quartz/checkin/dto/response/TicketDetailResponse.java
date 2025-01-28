package com.quartz.checkin.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.entity.TicketAttachment;
import com.quartz.checkin.repository.TicketAttachmentRepository;
import java.util.stream.Collectors;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketDetailResponse {

    private Long ticketId;
    private String title;
    private String firstCategory;
    private String secondCategory;
    private String username;
    private String manager;
    private String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd")
    private LocalDate dueDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime createdAt;

    private String priority;
    private String status;
    private List<Long> ticketAttachmentIds;

    public static TicketDetailResponse from(Ticket ticket, TicketAttachmentRepository attachmentRepository) {
        List<Long> attachmentIds = attachmentRepository.findByTicketId(ticket.getId())
                .stream()
                .map(TicketAttachment::getId)
                .collect(Collectors.toList());

        return TicketDetailResponse.builder()
                .ticketId(ticket.getId())
                .title(ticket.getTitle())
                .firstCategory(ticket.getFirstCategory().getName())
                .secondCategory(ticket.getSecondCategory().getName())
                .username(ticket.getUser().getUsername())
                .manager(ticket.getManager() != null ? ticket.getManager().getUsername() : null)
                .content(ticket.getContent())
                .dueDate(ticket.getDueDate())
                .createdAt(ticket.getCreatedAt())
                .priority(ticket.getPriority() != null ? ticket.getPriority().name() : null)
                .status(ticket.getStatus().name())
                .ticketAttachmentIds(attachmentIds)
                .build();
    }
}
