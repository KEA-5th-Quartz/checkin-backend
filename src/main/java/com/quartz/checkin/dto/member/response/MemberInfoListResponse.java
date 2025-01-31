package com.quartz.checkin.dto.member.response;

import com.quartz.checkin.entity.Member;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberInfoListResponse {

    private int page;
    private int size;
    private int totalPages;
    private long totalMembers;
    private List<MemberInfoResponse> members;

    public static MemberInfoListResponse from(Page<Member> memberPage) {
        List<MemberInfoResponse> members = memberPage.getContent().stream()
                .map(MemberInfoResponse::from)
                .toList();

        return MemberInfoListResponse.builder()
                .page(memberPage.getNumber() + 1)
                .size(memberPage.getSize())
                .totalPages(memberPage.getTotalPages())
                .totalMembers(memberPage.getTotalElements())
                .members(members)
                .build();
    }
}
