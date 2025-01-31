package com.quartz.checkin.dto.ticket.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
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
}