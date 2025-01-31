package com.quartz.checkin.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "logId", "logContent", "commentId", "memberId", "commentContent", "isImage", "attachmentUrl", "createdAt"})
public class ActivityResponse {
    private ActivityType type;

    private LocalDateTime createdAt;

    // LOG 타입일 때 사용
    private Long logId;

    private String logContent;

    // COMMENT 타입일 때 사용
    private Long commentId;

    private Long memberId;

    private String commentContent;

    private Boolean isImage;

    private String attachmentUrl;
}
