package com.quartz.checkin.dto.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UploadAttachmentsResponse {

    private Long attachmentId;
    private String url;
}
