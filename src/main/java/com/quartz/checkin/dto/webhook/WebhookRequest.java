package com.quartz.checkin.dto.webhook;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import org.springframework.http.HttpMethod;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebhookRequest {
    private String url;
    private HttpMethod method;
    private String text;
    private Task task;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Task {
        private String templateName;
        private List<String> assignees;
    }
}