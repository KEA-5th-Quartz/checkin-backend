package com.quartz.checkin.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentLikeResponse {
    @JsonProperty("is_liked")
    private Boolean isLiked;

    @JsonProperty("like_id")
    private Long id;

    @JsonProperty("comment_id")
    private Long commentId;
}