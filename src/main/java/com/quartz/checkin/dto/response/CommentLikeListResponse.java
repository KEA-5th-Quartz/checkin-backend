package com.quartz.checkin.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentLikeListResponse {
    @JsonProperty("ticket_id")
    private Long ticketId;

    @JsonProperty("comment_id")
    private Long commentId;

    @JsonProperty("total_likes")
    private int totalLikes;

    private List<LikesUserList> likes;
}
