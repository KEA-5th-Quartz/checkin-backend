package com.quartz.checkin.dto.response;

import com.quartz.checkin.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberInfoResponse {
    private Long memberId;
    private String username;
    private String email;
    private String profilePic;
    private String role;

    public static MemberInfoResponse from(Member member) {
        return MemberInfoResponse.builder()
                .memberId(member.getId())
                .username(member.getUsername())
                .email(member.getEmail())
                .profilePic(member.getProfilePic())
                .role(member.getRole().getValue())
                .build();
    }
}
