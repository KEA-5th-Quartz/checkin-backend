package com.quartz.checkin.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TicketCategoryChangedEvent {
    private Long ticketId;
    private String customId;
    private Long agitId;
    private Long userId;
    private String oldCategory;
    private String newCategory;
    private String logMessage;
}