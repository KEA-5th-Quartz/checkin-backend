package com.quartz.checkin.exception.custom;

import com.quartz.checkin.exception.ErrorCode;

public class EmailDuplicateException extends CustomException {

    public EmailDuplicateException() {
        super(ErrorCode.EMAIL_DUPLICATE);
    }
}
