package com.quartz.checkin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class TicketCreateResponse {
    private Long ticketId;
    private LocalDateTime createdAt;
}
