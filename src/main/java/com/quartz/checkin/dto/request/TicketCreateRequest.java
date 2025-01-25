package com.quartz.checkin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class TicketCreateRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotBlank
    private String firstCategory;

    @NotBlank
    private String secondCategory;

    @NotNull
    private LocalDate dueDate;
    private LocalDateTime createdAt = LocalDateTime.now();
    private List<Long> ticketAttachmentIds; // 첨부파일 ID 리스트 (nullable)
}

