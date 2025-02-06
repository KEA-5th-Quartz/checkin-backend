package com.quartz.checkin.entity;

import lombok.Getter;

@Getter
public enum AccessLogType {
    LOGIN_SUCCESS("로그인 성공"),
    WRONG_PASSWORD("패스워드 불일치");

    private final String value;

    AccessLogType(String value) {
        this.value = value;
    }
}
