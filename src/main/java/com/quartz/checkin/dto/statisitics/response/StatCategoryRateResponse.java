package com.quartz.checkin.dto.statisitics.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
public class StatCategoryRateResponse {

    private String userName;
    private List<StatCategoryCountResponse> state;

}