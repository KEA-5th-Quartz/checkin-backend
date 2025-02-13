package com.quartz.checkin.dto.stat.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StatClosedRateResponse {
    private int totalCount;
    private double closedRate;
    private int closedCount;
    private int unclosedCount;
}