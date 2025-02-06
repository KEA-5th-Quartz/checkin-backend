package com.quartz.checkin.dto.ticket.request;

import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketCommentRequest {

    @Size(max = 128, message = "댓글 내용은 최대 128자까지 입력할 수 있습니다.")
    private String content;
}
