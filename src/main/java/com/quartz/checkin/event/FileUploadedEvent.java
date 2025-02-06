package com.quartz.checkin.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FileUploadedEvent {
    private Long ticketId;
    private Long agitId;
    private String username;
}
