package com.quartz.checkin.dto.stat.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StatCategoryCountResponse {

    private String categoryName;
    private int ticketCount;

}