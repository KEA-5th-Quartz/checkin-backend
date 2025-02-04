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

    public static boolean isRole(String value) {
        for (Role role : Role.values()) {
            if (role.value.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static Role fromValue(String value) {
        for (Role role : Role.values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 권한입니다.");
    }
}