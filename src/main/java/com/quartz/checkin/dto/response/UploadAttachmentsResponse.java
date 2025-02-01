package com.quartz.checkin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UploadAttachmentsResponse {

    private Long attachmentId;
    private String url;
}
