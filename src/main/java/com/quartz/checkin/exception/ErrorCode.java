package com.quartz.checkin.exception;

import com.quartz.checkin.dto.response.ErrorResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED.value(), "COMMON_4010", "유효하지 않거나 만료된 accessToken입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED.value(), "COMMON_4011", "유효하지 않거나 만료된 refreshToken입니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN.value(), "COMMON_4030", "접근 권한이 없습니다."),
    UNAUTHENTICATED_ACCESS(HttpStatus.UNAUTHORIZED.value(), "COMMON_4031", "인증이 필요한 요청입니다."),
    VALIDATION_FAILURE(HttpStatus.BAD_REQUEST.value(), "COMMON_4090", "검증에 실패하였습니다."),

    EMAIL_DUPLICATE(HttpStatus.CONFLICT.value(), "MEMBER_4091", "이미 사용 중인 이메일입니다."),
    USERNAME_DUPLICATE(HttpStatus.CONFLICT.value(), "MEMBER_4090", "이미 사용 중인 사용자 이름입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "MEMBER_4040", "존재하지 않는 회원입니다."),
    WRONG_USERNAME_OR_PASSWORD(HttpStatus.UNAUTHORIZED.value(), "MEMBER_4013", "일치하는 회원 정보가 없습니다. 아이디 혹은 비밀번호를 다시 확인해주세요.");

    private final int status;
    private final String code;
    private final String message;

    public static ErrorResponse toResponse(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getStatus(), errorCode.getCode(), errorCode.getMessage());
    }

}
