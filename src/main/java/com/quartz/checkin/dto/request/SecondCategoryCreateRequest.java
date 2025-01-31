package com.quartz.checkin.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SecondCategoryCreateRequest {

    @NotNull
    private String name;
}
