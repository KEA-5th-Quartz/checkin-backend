package com.quartz.checkin.exception.custom;

import org.springframework.security.core.AuthenticationException;

public class InvalidAccessTokenException extends AuthenticationException {

    public InvalidAccessTokenException(String msg) {
        super(msg);
    }
}
