package com.quartz.checkin.dto.category.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class FirstCategoryUpdateRequest {

    @NotBlank
    private String firstCategory;
}
