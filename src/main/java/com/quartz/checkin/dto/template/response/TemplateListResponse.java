package com.quartz.checkin.dto.template.response;

import com.quartz.checkin.entity.Template;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@Builder
public class TemplateListResponse {

    private int page;
    private int size;
    private int totalPages;
    private long totalTemplates;
    private List<TemplateSimpleResponse> templates;

    public static TemplateListResponse from(Page<Template> templatePage) {
        List<TemplateSimpleResponse> templates = templatePage.getContent().stream()
                .map(TemplateSimpleResponse::from)
                .toList();

        return TemplateListResponse.builder()
                .page(templatePage.getNumber() + 1)
                .size(templatePage.getSize())
                .totalPages(templatePage.getTotalPages())
                .totalTemplates(templatePage.getTotalElements())
                .templates(templates)
                .build();
    }
}
