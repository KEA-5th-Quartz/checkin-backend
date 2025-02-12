package com.quartz.checkin.service;

import static com.quartz.checkin.common.PaginationUtils.validatePagination;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.converter.TicketResponseConverter;
import com.quartz.checkin.dto.ticket.response.ManagerTicketListResponse;
import com.quartz.checkin.dto.ticket.response.TicketDetailResponse;
import com.quartz.checkin.dto.ticket.response.TicketProgressResponse;
import com.quartz.checkin.dto.ticket.response.UserTicketListResponse;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Priority;
import com.quartz.checkin.entity.Role;
import com.quartz.checkin.entity.Status;
import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.entity.TicketAttachment;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.repository.TicketAttachmentRepository;
import com.quartz.checkin.repository.TicketRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketQueryServiceImpl implements TicketQueryService {

    private final TicketRepository ticketRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final MemberRepository memberRepository;


    @Override
    public TicketDetailResponse getTicketDetail(Long memberId, Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));

        boolean isManager = member.getRole() == Role.MANAGER;

        if (!isManager && !ticket.getUser().getId().equals(memberId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        List<TicketAttachment> attachments = ticketAttachmentRepository.findByTicketId(ticketId);
        return TicketDetailResponse.from(ticket, attachments);
    }

    @Override
    public ManagerTicketListResponse getManagerTickets(Long memberId, List<Status> statuses, List<String> usernames,
                                                       List<String> categories, List<Priority> priorities,
                                                       Boolean dueToday, Boolean dueThisWeek, int page, int size, String sortByCreatedAt) {

        Page<Ticket> ticketPage = fetchTickets(null, statuses, usernames, categories, priorities, dueToday, dueThisWeek, page, size, sortByCreatedAt);
        validatePagination(page, size, ticketPage.getTotalPages());
        return TicketResponseConverter.toManagerTicketListResponse(ticketPage);
    }

    @Override
    public UserTicketListResponse getUserTickets(Long userId, List<Status> statuses, List<String> usernames,
                                                 List<String> categories, List<Priority> priorities,
                                                 Boolean dueToday, Boolean dueThisWeek, int page, int size, String sortByCreatedAt) {
        Page<Ticket> ticketPage = fetchTickets(userId, statuses, usernames, categories, priorities, dueToday, dueThisWeek, page, size, sortByCreatedAt);
        validatePagination(page, size, ticketPage.getTotalPages());
        return TicketResponseConverter.toUserTicketListResponse(ticketPage);
    }

    @Override
    public ManagerTicketListResponse searchManagerTickets(Long memberId, String keyword, int page, int size, String sortByCreatedAt) {
        Page<Ticket> ticketPage = fetchSearchedTickets(null, keyword, page, size, sortByCreatedAt);
        validatePagination(page, size, ticketPage.getTotalPages());
        return TicketResponseConverter.toManagerTicketListResponse(ticketPage);
    }

    @Override
    public UserTicketListResponse searchUserTickets(Long memberId, String keyword, int page, int size, String sortByCreatedAt) {
        Page<Ticket> ticketPage = fetchSearchedTickets(memberId, keyword, page, size, sortByCreatedAt);
        validatePagination(page, size, ticketPage.getTotalPages());
        return TicketResponseConverter.toUserTicketListResponse(ticketPage);
    }

    @Override
    public TicketProgressResponse getManagerProgress(Long managerId) {
        return ticketRepository.getManagerProgress(managerId);
    }

    public Page<Ticket> fetchSearchedTickets(Long memberId, String keyword, int page, int size, String sortByCreatedAt) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.unsorted());
        return ticketRepository.fetchSearchedTickets(memberId, keyword, pageable, sortByCreatedAt);
    }

    public Page<Ticket> fetchTickets(Long memberId, List<Status> statuses, List<String> usernames,
                                     List<String> categories, List<Priority> priorities,
                                     Boolean dueToday, Boolean dueThisWeek, int page, int size, String sortByCreatedAt) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.unsorted());
        return ticketRepository.fetchTickets(memberId, statuses, usernames, categories, priorities, dueToday, dueThisWeek, pageable, sortByCreatedAt);
    }
}