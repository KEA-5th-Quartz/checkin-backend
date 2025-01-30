package com.quartz.checkin.dto.response;

public class StatCategoryTicketResponse {
    private String categoryName;
    private Long ticketCount;

    // Constructor
    public StatCategoryTicketResponse(String categoryName, Long ticketCount) {
        this.categoryName = categoryName;
        this.ticketCount = ticketCount;
    }

    // Getters and Setters
    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getTicketCount() {
        return ticketCount;
    }

    public void setTicketCount(Long ticketCount) {
        this.ticketCount = ticketCount;
    }
}