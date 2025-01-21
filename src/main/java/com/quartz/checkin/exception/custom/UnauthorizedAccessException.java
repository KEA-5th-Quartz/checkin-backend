package com.quartz.checkin.exception.custom;

import com.quartz.checkin.exception.ErrorCode;

public class UnauthorizedAccessException extends CustomException {

    public UnauthorizedAccessException() {
        super(ErrorCode.UNAUTHORIZED_ACCESS);
    }
}
