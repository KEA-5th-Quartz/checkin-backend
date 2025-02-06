package com.quartz.checkin.dto.statisitics.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class StatTotalProgressResponse {

    private String status;
    private int ticketCount;

}