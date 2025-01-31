package com.quartz.checkin.dto.comment.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentAttachmentResponse {
    private Long commentId;
    private Boolean isImage;
    private String attachmentUrl;
}
