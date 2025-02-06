package com.quartz.checkin.dto.ticket.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SoftDeletedTicketResponse {
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;
    private List<TicketDetail> tickets;

    @Getter
    @Setter
    public static class TicketDetail {
        private Long ticketId;
        private String title;
        private String firstCategory;
        private String secondCategory;
        private String manager;
        private String managerProfilePic;
        private String content;
        private String dueDate;
        private String priority;
        private String status;
    }
}
