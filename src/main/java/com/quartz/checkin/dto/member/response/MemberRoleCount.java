package com.quartz.checkin.dto.member.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberRoleCount {

    private Long totalUsers;
    private Long totalManagers;
    private Long totalAdmins;
}
