package com.quartz.checkin.dto.category.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class SecondCategoryUpdateRequest {

    @NotBlank
    private String secondCategory;

    @NotBlank
    @Size(max = 3)
    private String alias;

}
