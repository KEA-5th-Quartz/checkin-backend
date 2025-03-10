package com.quartz.checkin.dto.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.quartz.checkin.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(Include.NON_NULL)
public class ApiErrorResponse<T> {

    private int status;
    private T data;
    private String code;
    private String message;

    public static <T> ApiErrorResponse<T> createErrorResponse(ErrorCode errorCode) {
        return ApiErrorResponse.<T>builder()
                .status(errorCode.getStatus().value())
                .code(errorCode.getCode())
                .data(null)
                .message(errorCode.getMessage())
                .build();
    }

    public static <T> ApiErrorResponse<T> createErrorResponseWithData(ErrorCode errorCode, T data) {
        return ApiErrorResponse.<T>builder()
                .status(errorCode.getStatus().value())
                .code(errorCode.getCode())
                .data(data)
                .message(errorCode.getMessage())
                .build();
    }


}