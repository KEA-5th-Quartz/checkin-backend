package com.quartz.checkin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SecondCategoryUpdateRequest {

    @NotBlank
    private String secondCategory;
}
