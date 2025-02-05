package com.quartz.checkin.service;

import com.quartz.checkin.common.PaginationRequestUtils;
import com.quartz.checkin.dto.common.request.SimplePageRequest;
import com.quartz.checkin.dto.member.response.AccessLogListResponse;
import com.quartz.checkin.entity.MemberAccessLog;
import com.quartz.checkin.repository.MemberAccessLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAccessLogService {

    private final MemberAccessLogRepository memberAccessLogRepository;

    public AccessLogListResponse getAccessLogList(SimplePageRequest pageRequest) {

        Integer page = pageRequest.getPage();
        Integer size = pageRequest.getSize();

        PaginationRequestUtils.checkPageNumberAndPageSize(page, size);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());

        Page<MemberAccessLog> accessLogPage = memberAccessLogRepository.findAllJoinFetch(pageable);

        return AccessLogListResponse.from(accessLogPage);
    }



}
