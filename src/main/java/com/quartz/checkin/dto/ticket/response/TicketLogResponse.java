package com.quartz.checkin.dto.ticket.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.quartz.checkin.entity.TicketLog;
import com.quartz.checkin.entity.LogType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TicketLogResponse {

    private final Long logId;
    private final Long ticketId;
    private final LogType logType;
    private final String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime createdAt;

    public TicketLogResponse(TicketLog ticketLog) {
        this.logId = ticketLog.getId();
        this.ticketId = ticketLog.getTicket().getId();
        this.logType = ticketLog.getLogtype();
        this.content = ticketLog.getContent();
        this.createdAt = ticketLog.getCreatedAt();
    }
}
