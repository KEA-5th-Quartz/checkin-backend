package com.quartz.checkin.dto.category.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SecondCategoryCreateRequest {

    @NotBlank(message = "2차 카테고리 이름을 입력하세요")
    private String name;

    @NotBlank(message = "카테고리 약어을 입력하세요 (영어 대문자 3)")
    @Size(min = 3, max = 3)
    private String alias;
}
