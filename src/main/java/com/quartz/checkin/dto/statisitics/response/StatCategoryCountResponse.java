package com.quartz.checkin.dto.statisitics.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class StatCategoryCountResponse {

    private String categoryName;
    private int ticketCount;

}