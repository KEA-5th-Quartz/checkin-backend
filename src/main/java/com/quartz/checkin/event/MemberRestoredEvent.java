package com.quartz.checkin.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberRestoredEvent {

    String username;
}
