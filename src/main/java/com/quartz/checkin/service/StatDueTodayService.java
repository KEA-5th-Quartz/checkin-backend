package com.quartz.checkin.service;

import com.quartz.checkin.dto.response.StatDueTodayResponse;
import com.quartz.checkin.repository.StatDueTodayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class StatDueTodayService {

    private final StatDueTodayRepository statDueTodayRepository;

    public StatDueTodayResponse getDueTodayTickets(Long managerId) {
        return (StatDueTodayResponse) statDueTodayRepository.findDueTodayTickets(managerId)
                .orElseThrow(() -> new NoSuchElementException("해당 담당자의 진행중인 티켓이 없습니다."));
    }
}
