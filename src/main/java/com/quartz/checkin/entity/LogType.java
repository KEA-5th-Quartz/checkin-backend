package com.quartz.checkin.entity;

import lombok.Getter;

@Getter
public enum LogType {

    STATUS("상태 변경"),
    CATEGORY("카테고리 변경"),
    MANAGER("담당자 변경"),
    UPDATE("티켓 수정");

    private final String value;
    LogType(String value) { this.value = value; }
}
