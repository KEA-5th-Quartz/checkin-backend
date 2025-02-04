package com.quartz.checkin.entity;

import lombok.Getter;

@Getter
public enum Status {

    OPEN("생성"),
    IN_PROGRESS("진행 중"),
    CLOSED("완료");

    private final String value;

    Status(String value) {
        this.value = value;
    }
}

