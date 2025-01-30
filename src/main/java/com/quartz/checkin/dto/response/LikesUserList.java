package com.quartz.checkin.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LikesUserList {
    private Long memberId;

    private String memberName;
}
