package com.quartz.checkin.service;

import com.quartz.checkin.dto.response.StatDueTodayResponse;
import com.quartz.checkin.repository.StatDueTodayRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import java.util.Map;

@Service
public class StatDueTodayService {

    @Autowired
    private StatDueTodayRepository memberRepository;

    public StatDueTodayResponse getTicketCountByManagerId(Long managerId) {
        List<Map<String, Object>> result = memberRepository.findTicketCountByManagerId(managerId);

        if (result.isEmpty()) {
            return new StatDueTodayResponse("담당자 없음", 0);
        }

        Map<String, Object> data = result.get(0);
        String username = (String) data.get("username");
        int ticketCount = ((Number) data.get("ticket_count")).intValue();

        return new StatDueTodayResponse(username, ticketCount);
    }
}