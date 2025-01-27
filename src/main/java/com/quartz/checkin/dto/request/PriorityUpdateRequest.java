package com.quartz.checkin.dto.request;

import com.quartz.checkin.entity.Priority;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class PriorityUpdateRequest {

    @NotNull
    private Priority priority;
}
