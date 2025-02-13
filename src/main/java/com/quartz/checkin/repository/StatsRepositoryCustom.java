package com.quartz.checkin.repository;

import com.quartz.checkin.dto.stat.response.StatCategoryRateResponse;
import com.quartz.checkin.dto.stat.response.StatTotalProgressResultResponse;
import java.util.List;

public interface StatsRepositoryCustom {
    List<StatCategoryRateResponse> findStatsByCategory();
    StatTotalProgressResultResponse findStatTotalProgress();
}
