package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.dto.request.TicketCreateRequest;
import com.quartz.checkin.dto.response.*;
import com.quartz.checkin.entity.*;
import com.quartz.checkin.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketCrudServiceImpl implements TicketCrudService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;

    @Transactional
    @Override
    public TicketCreateResponse createTicket(Long memberId, TicketCreateRequest request) {
        // 사용자 조회
        Member user = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.MEMBER_NOT_FOUND));

        // 1차 카테고리 조회 (parent_id가 NULL인 경우)
        Category firstCategory = categoryRepository.findByNameAndParentIsNull(request.getFirstCategory())
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST));

        // 2차 카테고리 조회 (해당 1차 카테고리의 자식 카테고리)
        Category secondCategory = categoryRepository.findByNameAndParent(request.getSecondCategory(), firstCategory)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_SECOND));
        // 티켓 생성 및 저장
        Ticket ticket = Ticket.builder()
                .user(user)
                .firstCategory(firstCategory)
                .secondCategory(secondCategory)
                .title(request.getTitle())
                .content(request.getContent())
                .status(Status.OPEN)
                .dueDate(request.getDueDate())
                .build();
        ticketRepository.save(ticket);

        return new TicketCreateResponse(ticket.getId());
    }

    @Transactional
    @Override
    public TicketDetailResponse getTicketDetail(Long memberId, Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ApiException(ErrorCode.TICKET_NOT_FOUND));

        return TicketDetailResponse.from(ticket);
    }

    @Transactional
    @Override
    public ManagerTicketListResponse getManagerTickets(Long memberId, Status status, String username, String category, Priority priority, int page, int size) {
        if (page < 1) throw new ApiException(ErrorCode.INVALID_PAGE_NUMBER);
        if (size <= 0) throw new ApiException(ErrorCode.INVALID_PAGE_SIZE);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Ticket> ticketPage;
        if (status == null && category == null && username == null && priority == null) {
            // 전체 티켓 조회
            ticketPage = ticketRepository.findAllTickets(pageable);
        } else {
            // 필터링된 티켓 조회
            ticketPage = ticketRepository.findTickets(status, username, category, priority, pageable);
        }

        List<ManagerTicketSummaryResponse> ticketList = ticketPage.getContent().stream()
                .map(ManagerTicketSummaryResponse::from)
                .collect(Collectors.toList());

        return new ManagerTicketListResponse(
                ticketPage.getNumber() + 1,
                ticketPage.getSize(),
                ticketPage.getTotalPages(),
                (int) ticketPage.getTotalElements(),
                ticketList
        );
    }

    @Transactional
    @Override
    public UserTicketListResponse getUserTickets(Long memberId, Status status, String username, String category, int page, int size) {
        if (page < 1) throw new ApiException(ErrorCode.INVALID_PAGE_NUMBER);
        if (size <= 0) throw new ApiException(ErrorCode.INVALID_PAGE_SIZE);

        // page 0-based 처리
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Ticket> ticketPage = ticketRepository.findUserTickets(memberId, status, username, category, pageable);

        List<UserTicketSummaryResponse> ticketList = ticketPage.getContent().stream()
                .map(UserTicketSummaryResponse::from)
                .collect(Collectors.toList());

        return new UserTicketListResponse(
                ticketPage.getNumber() + 1, // 1-based 반환
                ticketPage.getSize(),
                ticketPage.getTotalPages(),
                (int) ticketPage.getTotalElements(),
                ticketList
        );
    }

}
