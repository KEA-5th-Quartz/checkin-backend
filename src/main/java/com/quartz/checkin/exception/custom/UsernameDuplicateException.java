package com.quartz.checkin.exception.custom;

import com.quartz.checkin.exception.ErrorCode;

public class UsernameDuplicateException extends CustomException {

    public UsernameDuplicateException() {
        super(ErrorCode.USERNAME_DUPLICATE);
    }
}
