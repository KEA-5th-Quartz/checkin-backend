package com.quartz.checkin.dto.ticket.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PermanentlyDeleteTicketsRequest {

    @NotEmpty(message = "삭제할 티켓 ID 목록은 비어 있을 수 없습니다.")
    private List<Long> ticketIds;
}

