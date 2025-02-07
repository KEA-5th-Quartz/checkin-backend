package com.quartz.checkin.dto.ticket.response;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;

@Getter
public class DeletedTicketDetail {
    private final Long ticketId;
    private final String customId;
    private final String title;
    private final String manager;
    private final String managerProfilePic;
    private final String content;
    private final String dueDate;
    private final String status;

    @QueryProjection
    public DeletedTicketDetail(Long ticketId, String customId, String title, String manager,
                               String managerProfilePic, String content, String dueDate, String status) {
        this.ticketId = ticketId;
        this.customId = customId;
        this.title = title;
        this.manager = manager;
        this.managerProfilePic = managerProfilePic;
        this.content = content;
        this.dueDate = dueDate;
        this.status = status;
    }
}
