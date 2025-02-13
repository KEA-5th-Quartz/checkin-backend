package com.quartz.checkin.dto.stat.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class StatCategoryTicketResponse {

    private String categoryName;
    private Long ticketCount;

}