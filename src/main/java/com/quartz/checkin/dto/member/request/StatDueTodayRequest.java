package com.quartz.checkin.dto.request;
import java.time.LocalDate;

public class StatDueTodayRequest {
    private Long managerId;
    private LocalDate dueDate;

    // Getters and Setters
    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
}