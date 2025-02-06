package com.quartz.checkin.dto.member.response;

import com.quartz.checkin.entity.AccessLogType;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.MemberAccessLog;
import com.quartz.checkin.entity.Role;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AccessLogResponse {
    private Long memberAccessLogId;
    private String username;
    private String email;
    private String accessLogType;
    private Role role;
    private String ip;
    private LocalDateTime createdAt;

    public static AccessLogResponse from(MemberAccessLog memberAccessLog) {
        Member member = memberAccessLog.getMember();
        return AccessLogResponse.builder()
                .memberAccessLogId(memberAccessLog.getId())
                .username(member.getUsername())
                .email(member.getEmail())
                .accessLogType(memberAccessLog.getAccessLogType().getValue())
                .role(member.getRole())
                .ip(memberAccessLog.getIp())
                .createdAt(memberAccessLog.getCreatedAt())
                .build();
    }
}
