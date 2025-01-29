package com.quartz.checkin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FirstCategoryUpdateRequest {

    @NotBlank
    private String firstCategory;
}
