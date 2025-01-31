package com.quartz.checkin.dto.request;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StatProgressRequest {
    // Getters and Setters
    private String type; // "WEEK", "MONTH", "QUARTER"

}