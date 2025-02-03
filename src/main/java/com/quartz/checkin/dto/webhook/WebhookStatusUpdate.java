package com.quartz.checkin.dto.webhook;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebhookStatusUpdate {
    private int status;
}
