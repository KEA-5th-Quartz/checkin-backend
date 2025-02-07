package com.quartz.checkin.dto.category.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class FirstCategoryPatchRequest {

    @NotBlank(message = "카테고리 이름을 입력하세요")
    private String firstCategory;
}
