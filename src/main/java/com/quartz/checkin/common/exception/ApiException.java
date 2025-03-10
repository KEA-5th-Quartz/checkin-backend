package com.quartz.checkin.common.exception;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private ErrorCode errorCode;

    public ApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ApiException(String message) {
        super(message);
    }
}
