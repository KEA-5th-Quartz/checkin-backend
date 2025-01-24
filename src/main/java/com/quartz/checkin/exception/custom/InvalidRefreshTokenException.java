package com.quartz.checkin.exception.custom;

import com.quartz.checkin.exception.ErrorCode;

public class InvalidRefreshTokenException extends CustomException {

    public InvalidRefreshTokenException() {
        super(ErrorCode.INVALID_REFRESH_TOKEN);
    }
}
