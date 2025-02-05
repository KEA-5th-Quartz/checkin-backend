package com.quartz.checkin.service;

import com.quartz.checkin.common.PaginationRequestUtils;
import com.quartz.checkin.dto.common.request.SimplePageRequest;
import com.quartz.checkin.dto.member.response.AccessLogListResponse;
import com.quartz.checkin.entity.AccessLogType;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.MemberAccessLog;
import com.quartz.checkin.repository.MemberAccessLogRepository;
import jakarta.servlet.http.HttpServletRequest;
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
    private final MemberService memberService;

    public AccessLogListResponse getAccessLogList(SimplePageRequest pageRequest) {

        Integer page = pageRequest.getPage();
        Integer size = pageRequest.getSize();

        PaginationRequestUtils.checkPageNumberAndPageSize(page, size);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").descending());

        Page<MemberAccessLog> accessLogPage = memberAccessLogRepository.findAllJoinFetch(pageable);

        return AccessLogListResponse.from(accessLogPage);
    }

    @Transactional
    public void writeLoginSuccessAccessLog(Long memberId, String clientIp) {
        Member member = memberService.getMemberByIdOrThrow(memberId);

        MemberAccessLog memberAccessLog = MemberAccessLog.builder()
                .member(member)
                .accessLogType(AccessLogType.LOGIN_SUCCESS)
                .ip(clientIp)
                .build();

        memberAccessLogRepository.save(memberAccessLog);
    }

    @Transactional
    public void writeWrongPasswordAccessLog(String username, String clientIp) {
        Member member = memberService.getMemberByUsernameOrThrow(username);

        MemberAccessLog memberAccessLog = MemberAccessLog.builder()
                .member(member)
                .accessLogType(AccessLogType.WRONG_PASSWORD)
                .ip(clientIp)
                .build();

        memberAccessLogRepository.save(memberAccessLog);

    }




    public String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }


}
