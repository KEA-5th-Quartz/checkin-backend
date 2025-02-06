package com.quartz.checkin.dto.statisitics.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class StatDueTodayResponse {

    private String userName;
    private int ticketCount;

}