package com.quartz.checkin.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StatCategoryTicketResponse {
    // Getters and Setters
    private String categoryName;
    private Long ticketCount;

    // Constructor
    public StatCategoryTicketResponse(String categoryName, Long ticketCount) {
        this.categoryName = categoryName;
        this.ticketCount = ticketCount;
    }

}