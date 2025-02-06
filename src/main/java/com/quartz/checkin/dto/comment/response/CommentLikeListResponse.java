package com.quartz.checkin.dto.comment.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonPropertyOrder({"ticketId", "commentId", "totalLikes", "likes"})
public class CommentLikeListResponse {
    private String ticketId;

    private Long commentId;

    private int totalLikes;

    private List<LikesUserList> likes;
}
