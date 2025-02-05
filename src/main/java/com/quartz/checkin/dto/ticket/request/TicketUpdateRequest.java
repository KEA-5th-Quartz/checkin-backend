package com.quartz.checkin.dto.ticket.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;

@Getter
public class TicketUpdateRequest {
    @Size(max = 25, message = "제목은 최대 512자까지 입력할 수 있습니다.")
    private String title;
    @Size(max = 256, message = "내용은 최대 256자까지 입력할 수 있습니다.")
    private String content;
    private String firstCategory;
    private String secondCategory;
    private LocalDate dueDate;
    private List<Long> attachmentIds;
}
