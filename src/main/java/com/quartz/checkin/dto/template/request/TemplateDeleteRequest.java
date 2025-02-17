package com.quartz.checkin.dto.template.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TemplateDeleteRequest {

    @NotNull(message = "삭제할 템플릿들의 ID는 필수 입력값입니다.")
    @Size(min = 1, message = "삭제할 템플릿 ID는 최소 1개 이상이어야 합니다.")
    List<Long> templateIds;
}
