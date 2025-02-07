package com.quartz.checkin.dto.category.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class FirstCategoryUpdateRequest {

    @NotBlank(message = "카테고리 이름을 입력하세요")
    private String name;

    @NotBlank
    @Size(min = 2, max = 4, message = "카테고리 약어을 입력하세요 (영어 대문자 2~4)")
    private String alias;

    @NotBlank
    @Size(max = 256, message = "요청 사항에 대한 가이드라인을 적어주세요."
            + "ex>  인프라 점검 요청 시, 점검 대상(서버/네트워크)과 주요 증상을 상세히 기재해주세요.")
    private String contentGuide;
}
