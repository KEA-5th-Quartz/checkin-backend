package com.quartz.checkin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
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

    private List<Long> attachmentIds = new ArrayList<>();
}