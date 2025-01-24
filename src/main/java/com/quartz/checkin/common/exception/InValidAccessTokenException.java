package com.quartz.checkin.common.exception;

import org.springframework.security.core.AuthenticationException;

public class InValidAccessTokenException extends AuthenticationException {

    public InValidAccessTokenException() {
        super("유효하지 않은 토큰입니다.");
    }
}
