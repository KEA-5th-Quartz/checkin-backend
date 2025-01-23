package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiCode;
import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.dto.request.TicketRequest;
import com.quartz.checkin.dto.response.TicketResponse;
import com.quartz.checkin.entity.*;
import com.quartz.checkin.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;

    @Transactional
    @Override
    public TicketResponse createTicket(Long memberId, TicketRequest request) {
        // 사용자 조회
        Member user = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(ApiCode.MEMBER_NOT_FOUND));

        // 1차 카테고리 조회 (parent_id가 NULL인 경우)
        Category firstCategory = categoryRepository.findByNameAndParentIsNull(request.getFirstCategory())
                .orElseThrow(() -> new ApiException(ApiCode.CATEGORY_NOT_FOUND_FIRST));

        // 2차 카테고리 조회 (해당 1차 카테고리의 자식 카테고리)
        Category secondCategory = categoryRepository.findByNameAndParent(request.getSecondCategory(), firstCategory)
                .orElseThrow(() -> new ApiException(ApiCode.CATEGORY_NOT_FOUND_SECOND));

        // created_at이 null이면 현재 시간으로 설정
        LocalDateTime createdAt = request.getCreatedAt() != null ? request.getCreatedAt() : LocalDateTime.now();

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

        return new TicketResponse(ticket.getId(), createdAt);
    }
}
