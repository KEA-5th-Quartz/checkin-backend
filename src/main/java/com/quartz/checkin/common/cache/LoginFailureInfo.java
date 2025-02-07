package com.quartz.checkin.common.cache;

import lombok.Getter;

@Getter
public class LoginFailureInfo {
    private int count;
    private final long firstFailureTime;

    public LoginFailureInfo() {
        this.count = 1;
        this.firstFailureTime = System.currentTimeMillis();
    }

    public void increment() {
        this.count++;
    }

}
