package com.quartz.checkin.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivityResponse {
    private ActivityType type;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    // LOG 타입일 때 사용
    @JsonProperty("log_id")
    private Long logId;

    @JsonProperty("log_content")
    private String logContent;

    // COMMENT 타입일 때 사용
    @JsonProperty("comment_id")
    private Long commentId;

    @JsonProperty("member_id")
    private Long memberId;

    @JsonProperty("comment_content")
    private String commentContent;

    @JsonProperty("attachment_url")
    private String attachment;
}
