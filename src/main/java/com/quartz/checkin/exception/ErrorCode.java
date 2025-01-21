package com.quartz.checkin.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    UNAUTHORIZED_ACCESS(HttpStatus.BAD_REQUEST.value(), "COMMON_4030", "접근 권한이 없습니다.");
    private final int status;
    private final String code;
    private final String message;

}
