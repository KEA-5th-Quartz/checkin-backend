package com.quartz.checkin.event;

import com.quartz.checkin.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommentAddedEvent {
    private Long id;
    private String customId;
    private Long agitId;
    private Comment comment;
}