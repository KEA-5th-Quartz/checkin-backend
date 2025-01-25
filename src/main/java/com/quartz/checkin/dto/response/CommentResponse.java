package com.quartz.checkin.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentResponse {
    @JsonProperty("comment_id")
    private Long id;

}