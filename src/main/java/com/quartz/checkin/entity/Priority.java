package com.quartz.checkin.entity;

import lombok.Getter;

@Getter
public enum Priority {

    EMERGENCY("긴급"),
    HIGH("높음"),
    MEDIUM("보통"),
    LOW("낮음");

    private final String value;

    Priority(String value) {
        this.value = value;
    }

}
