package com.quartz.checkin.dto.statisitics.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class StatCategoryCountResponse {
    // Getters and Setters
    private String name;        // 카테고리 이름 (예: "VM", "KE")
    private int ticket_count;    // 티켓 수

    // 생성자
    public StatCategoryCountResponse(String name, int ticket_count) {
        this.name = name;
        this.ticket_count = ticket_count;
    }

}