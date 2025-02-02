package com.quartz.checkin.dto.template.response;

import com.quartz.checkin.entity.Template;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TemplateSimpleResponse {
    private Long templateId;
    private String title;
    private String firstCategory;
    private String secondCategory;
    private String content;

    public static TemplateSimpleResponse from(Template template) {
        return TemplateSimpleResponse.builder()
                .templateId(template.getId())
                .title(template.getTitle())
                .firstCategory(template.getFirstCategory().getName())
                .secondCategory(template.getSecondCategory().getName())
                .content(template.getContent())
                .build();
    }
}
