package com.quartz.checkin.service;

import com.quartz.checkin.dto.request.StatClosedRateRequest;
import com.quartz.checkin.dto.response.StatClosedRateResponse;
import com.quartz.checkin.repository.StatClosedRateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StatClosedRateService {

    @Autowired
    private StatClosedRateRepository statClosedRateRepository;

    public StatClosedRateResponse getCompletionRate(StatClosedRateRequest request) {
        // Repository에서 완료율 조회
        Optional<Double> closedRateOptional = statClosedRateRepository.findCompletionRateByType(request);

        // 결과가 없으면 기본값 0.0 반환
        double closedRate = closedRateOptional.orElse(0.0);

        // 응답 DTO 생성
        return new StatClosedRateResponse(closedRate);
    }
}