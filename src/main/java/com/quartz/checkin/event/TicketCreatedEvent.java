package com.quartz.checkin.event;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
@Data
@AllArgsConstructor
public class TicketCreatedEvent {
    private Long id;
    private String customId;
    private Long userId;
    private Long agitId;
    private String title;
    private String content;
    @Getter
    private final List<String> assignees;

    public TicketCreatedEvent(Long id, String customId, Long userId, String title, String content, List<String> assignees) {
        this.id = id;
        this.customId = customId;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.assignees = assignees;
    }

}