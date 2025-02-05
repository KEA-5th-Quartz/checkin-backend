package com.quartz.checkin.dto.ticket.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TicketCreateRequest {

    @NotBlank
    @Size(max = 25, message = "제목은 최대 25자까지 입력할 수 있습니다.")
    private String title;

    @NotBlank
    @Size(max = 256, message = "내용은 최대 256자까지 입력할 수 있습니다.")
    private String content;

    @NotBlank
    private String firstCategory;

    @NotBlank
    private String secondCategory;

    @NotNull
    private LocalDate dueDate;

    private List<Long> attachmentIds = new ArrayList<>();
}