package com.quartz.checkin.dto.comment.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LikesUserList {
    private Long memberId;

    private String username;
}
