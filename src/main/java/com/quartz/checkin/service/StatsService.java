package com.quartz.checkin.service;

import com.quartz.checkin.dto.stat.response.StatCategoryRateResponse;
import com.quartz.checkin.dto.stat.response.StatTotalProgressResultResponse;
import com.quartz.checkin.repository.StatsRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final StatsRepository statsRepository;

    public List<StatCategoryRateResponse> getStatsByCategory() {
        return statsRepository.findStatsByCategory();
    }

    public StatTotalProgressResultResponse getStatTotalProgress() {
        return statsRepository.findStatTotalProgress();
    }

    public List<StatCategoryRateResponse> getStatsByManager(String period) {
        return statsRepository.findStatsByManager(period);
    }


}