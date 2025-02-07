package com.quartz.checkin.common;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;

public class PaginationRequestUtils {

    public static void checkPageNumberAndPageSize(Integer page, Integer size) {
        if (page == null) {
            throw new ApiException(ErrorCode.INVALID_PAGE_NUMBER);
        }

        if (size == null) {
            throw new ApiException(ErrorCode.INVALID_PAGE_SIZE);
        }
    }
}
