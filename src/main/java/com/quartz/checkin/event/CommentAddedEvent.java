package com.quartz.checkin.event;

import com.quartz.checkin.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommentAddedEvent {
    private Long ticketId;
    private Long agitId;
    private Comment comment;
}