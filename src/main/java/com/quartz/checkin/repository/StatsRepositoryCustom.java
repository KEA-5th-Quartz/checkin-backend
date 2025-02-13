package com.quartz.checkin.repository;

import com.quartz.checkin.dto.stat.response.StatCategoryRateResponse;
import com.quartz.checkin.dto.stat.response.StatClosedRateResponse;
import com.quartz.checkin.dto.stat.response.StatTotalProgressResultResponse;
import java.util.List;

public interface StatsRepositoryCustom {
    List<StatCategoryRateResponse> findStatsByCategory();
    StatTotalProgressResultResponse findStatTotalProgress();
    List<StatCategoryRateResponse> findStatsByManager(String period);
    StatClosedRateResponse findClosedRate(String period);
}
