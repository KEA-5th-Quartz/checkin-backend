package com.quartz.checkin.dto.webhook;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WebhookAssigneesUpdate {
    private List<String> assignees;
}

