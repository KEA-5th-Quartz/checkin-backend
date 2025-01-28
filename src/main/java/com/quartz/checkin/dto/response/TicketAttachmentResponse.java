package com.quartz.checkin.dto.response;

import com.quartz.checkin.entity.TicketAttachment;

public record TicketAttachmentResponse(Long attachmentId, String url) {

    public static TicketAttachmentResponse from(TicketAttachment attachment) {
        return new TicketAttachmentResponse(attachment.getId(), attachment.getUrl());
    }
}
