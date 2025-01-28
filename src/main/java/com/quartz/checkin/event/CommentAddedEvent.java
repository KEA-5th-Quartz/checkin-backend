package com.quartz.checkin.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommentAddedEvent {
    private Long ticketId;
    private Long commenterId;
    private Long userId;
    private Long managerId;
    private String comment;
}