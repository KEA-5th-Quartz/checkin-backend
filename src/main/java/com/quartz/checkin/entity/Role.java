package com.quartz.checkin.entity;

import lombok.Getter;

@Getter
public enum Role {

    USER("USER"),
    MANAGER("MANAGER"),
    ADMIN("ADMIN");

    private final String value;

    Role(String value) {
        this.value = value;
    }
}