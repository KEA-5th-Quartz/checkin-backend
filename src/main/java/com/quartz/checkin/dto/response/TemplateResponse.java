package com.quartz.checkin.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TemplateResponse {
    private Long id;
    private String title;
    private String firstCategory;
    private String secondCategory;
    private String content;
    private List<UploadAttachmentsResponse> attachmentIds;

}
