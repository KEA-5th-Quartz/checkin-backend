package com.quartz.checkin.repository;

import com.quartz.checkin.dto.ticket.response.TicketProgressResponse;
import com.quartz.checkin.entity.Priority;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import com.querydsl.core.BooleanBuilder;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TicketRepositoryCustom {
    TicketProgressResponse getManagerProgress(Long memberId);
    Page<Ticket> fetchSearchedTickets(Long memberId, String keyword, Pageable pageable, String sortByCreatedAt);

    Page<Ticket> fetchTickets(Long memberId, List<Status> statuses, List<String> usernames,
                              List<String> categories, List<Priority> priorities,
                              Boolean dueToday, Boolean dueThisWeek, Pageable pageable, String sortByCreatedAt);

    long getTotalCount(BooleanBuilder whereClause);
    String findLastTicketId(String prefix);
}
