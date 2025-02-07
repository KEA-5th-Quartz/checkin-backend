package com.quartz.checkin.dto.ticket.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AttachmentResponse {
    private Long attachmentId;
    private String fileName;
    private String url;
}
