package com.quartz.checkin.dto.member.response;

import com.quartz.checkin.entity.MemberAccessLog;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@AllArgsConstructor
@Builder
public class AccessLogListResponse {
    private int page;
    private int size;
    private int totalPages;
    private long totalAccessLogs;
    private List<AccessLogResponse> accessLogs;

    public static AccessLogListResponse from(Page<MemberAccessLog> accessLogPage) {
        List<AccessLogResponse> accessLogs =  accessLogPage.getContent().stream()
                .map(AccessLogResponse::from)
                .toList();

        return AccessLogListResponse.builder()
                .page(accessLogPage.getNumber() + 1)
                .size(accessLogPage.getSize())
                .totalPages(accessLogPage.getTotalPages())
                .totalAccessLogs(accessLogPage.getTotalElements())
                .accessLogs(accessLogs)
                .build();
    }
}
