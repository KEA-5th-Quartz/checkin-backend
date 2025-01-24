package com.quartz.checkin.exception.custom;

import com.quartz.checkin.exception.ErrorCode;

public class MemberNotFoundException extends CustomException {

    public MemberNotFoundException() {
        super(ErrorCode.MEMBER_NOT_FOUND);
    }
}
