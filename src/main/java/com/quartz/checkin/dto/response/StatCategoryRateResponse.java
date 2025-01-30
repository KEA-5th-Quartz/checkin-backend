package com.quartz.checkin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class StatCategoryRateResponse {
    private String username;
    private List<CategoryState> state;

    @Getter
    @AllArgsConstructor
    public static class CategoryState {
        private String name;
        private int ticketCount;
    }
}
