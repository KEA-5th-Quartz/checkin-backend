package com.quartz.checkin.util;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.quartz.checkin.common.exception.ErrorCode;
import org.springframework.test.web.servlet.ResultMatcher;

public class ErrorResponseMatchers {

    public static ResultMatcher errorResponse(ErrorCode errorCode) {
        return result -> {
            jsonPath("$.status", is(errorCode.getStatus().value())).match(result);
            jsonPath("$.code", is(errorCode.getCode())).match(result);
            jsonPath("$.message", notNullValue()).match(result);
        };
    }

}
