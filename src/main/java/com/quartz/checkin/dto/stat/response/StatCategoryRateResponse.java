package com.quartz.checkin.dto.stat.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StatCategoryRateResponse {

    private String userName;
    private List<StatCategoryCountResponse> state;

}