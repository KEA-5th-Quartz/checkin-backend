package com.quartz.checkin.common;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;

public class PaginationUtils {
    public static void validatePagination(int page, int size, int totalPages) {
        if (page < 1) throw new ApiException(ErrorCode.INVALID_PAGE_NUMBER);
        if (size != 20 && size != 50 && size != 100) throw new ApiException(ErrorCode.INVALID_PAGE_SIZE);
        if (page > totalPages && totalPages > 0) {
            throw new ApiException(ErrorCode.INVALID_PAGE_NUMBER);
        }
    }
}
