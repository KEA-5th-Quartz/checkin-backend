package com.quartz.checkin.dto.ticket.request;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TicketUpdateRequest {
    private String title;
    private String content;
    private String firstCategory;
    private String secondCategory;
    private LocalDate dueDate;
    private List<Long> attachmentIds;
}
