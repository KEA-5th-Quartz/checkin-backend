package com.quartz.checkin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SecondCategoryUpdateRequest {

    @NotBlank
    private String secondCategory;
}
