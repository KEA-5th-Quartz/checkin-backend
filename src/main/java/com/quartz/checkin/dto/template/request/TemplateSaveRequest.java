package com.quartz.checkin.dto.template.request;


import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

// 생성과 업데이트 처리
@Getter
@AllArgsConstructor
public class TemplateSaveRequest {
    @NotBlank(message = "제목은 필수 입력값입니다.")
    private String title;

    @NotBlank(message = "1차 카테고리는 필수 입력값입니다.")
    private String firstCategory;

    @NotBlank(message = "2차 카테고리는 필수 입력값입니다.")
    private String secondCategory;

    @NotBlank(message = "본문은 필수 입력값입니다.")
    private String content;

    private List<Long> attachmentIds = new ArrayList<>();
}
