package com.quartz.checkin.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public class LikesUserList {
    @JsonProperty("member_id")
    private Long memberId;

    @JsonProperty("member_name")
    private String memberName;
}
