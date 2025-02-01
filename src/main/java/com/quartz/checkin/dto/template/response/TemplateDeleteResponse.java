package com.quartz.checkin.dto.template.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TemplateDeleteResponse {

    private List<TemplateIdResponse> deletedTemplates;
}
