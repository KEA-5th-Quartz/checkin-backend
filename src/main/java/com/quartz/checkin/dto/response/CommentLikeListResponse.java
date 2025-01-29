package com.quartz.checkin.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonPropertyOrder({"ticket_id", "comment_id", "total_likes", "likes"})
public class CommentLikeListResponse {
    @JsonProperty("ticket_id")
    private Long ticketId;

    @JsonProperty("comment_id")
    private Long commentId;

    @JsonProperty("total_likes")
    private int totalLikes;

    private List<LikesUserList> likes;
}
