package com.quartz.checkin.dto.template.response;

import com.quartz.checkin.dto.common.response.UploadAttachmentsResponse;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TemplateDetailResponse {
    private Long templateId;
    private String title;
    private String firstCategory;
    private String secondCategory;
    private String content;
    private List<UploadAttachmentsResponse> attachmentIds;

}
