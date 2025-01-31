package com.quartz.checkin.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PasswordResetMailEvent {
    private final Long id;
    private final String email;
    private final String passwordResetToken;
}
