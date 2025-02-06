package com.quartz.checkin.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AgitPostCreateEvent {
    private String ticketId;
    private String title;
    private String content;
    private String username;
}

