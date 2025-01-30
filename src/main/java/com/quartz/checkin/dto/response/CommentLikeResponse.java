package com.quartz.checkin.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentLikeResponse {
    private Boolean isLiked;

    private Long likeId;

    private Long commentId;
}