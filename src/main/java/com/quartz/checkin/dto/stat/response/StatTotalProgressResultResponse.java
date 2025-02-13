package com.quartz.checkin.dto.stat.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StatTotalProgressResultResponse {
    private int totalCount;
    private List<StatTotalProgressResponse> result;
}
