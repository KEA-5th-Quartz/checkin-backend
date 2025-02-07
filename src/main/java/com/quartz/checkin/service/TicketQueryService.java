package com.quartz.checkin.service;

import com.quartz.checkin.dto.ticket.response.ManagerTicketListResponse;
import com.quartz.checkin.dto.ticket.response.TicketDetailResponse;
import com.quartz.checkin.dto.ticket.response.TicketProgressResponse;
import com.quartz.checkin.dto.ticket.response.UserTicketListResponse;
import com.quartz.checkin.entity.Priority;
import com.quartz.checkin.entity.Status;
import java.util.List;

public interface TicketQueryService {

    TicketDetailResponse getTicketDetail(Long memberId, Long ticketId);

    ManagerTicketListResponse getManagerTickets(Long memberId, List<Status> statuses, List<String> usernames,
                                                List<String> categories, List<Priority> priorities,
                                                Boolean dueToday, Boolean dueThisWeek, int page, int size, String sortByCreatedAt);

    UserTicketListResponse getUserTickets(Long memberId, List<Status> statuses, List<String> usernames,
                                          List<String> categories, List<Priority> priorities,
                                          Boolean dueToday, Boolean dueThisWeek, int page, int size, String sortByCreatedAt);

    ManagerTicketListResponse searchManagerTickets(Long memberId, String keyword, int page, int size, String sortByCreatedAt);

    UserTicketListResponse searchUserTickets(Long memberId, String keyword, int page, int size, String sortByCreatedAt);

    TicketProgressResponse getManagerProgress(Long memberId);
}
