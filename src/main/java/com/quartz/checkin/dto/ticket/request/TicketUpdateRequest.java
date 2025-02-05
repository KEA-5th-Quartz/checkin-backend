package com.quartz.checkin.dto.ticket.request;

import java.time.LocalDate;
import java.util.List;
import lombok.Getter;

@Getter
public class TicketUpdateRequest {
    private String title;
    private String content;
    private String firstCategory;
    private String secondCategory;
    private LocalDate dueDate;
    private List<Long> attachmentIds;
}
