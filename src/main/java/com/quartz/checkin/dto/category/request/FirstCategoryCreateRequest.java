package com.quartz.checkin.dto.category.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FirstCategoryCreateRequest {
    @NotNull
    private String name;
}
