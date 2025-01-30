package com.quartz.checkin.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberRegisteredEvent {
    private final String email;
    private final String tempPassword;
}
