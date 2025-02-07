package com.quartz.checkin.service;

import java.util.List;

public interface TicketTrashService {
    void restoreTickets(Long memberId, List<Long> ticketIds);
    void purgeTickets(Long memberId, List<Long> ticketIds);
}
