package com.quartz.checkin.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ApiCode {
    // 공통 예외
    OK(HttpStatus.OK, "COMMON_2000", "OK"),
    INVALID_DATA(HttpStatus.BAD_REQUEST, "COMMON_4000", "필수로 요구되는 데이터가 비어있거나 규칙에 맞지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_4010", "유효하지 않거나 만료된 토큰입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_4030", "접근 권한이 없습니다."),
    READ_ONLY_ACCESS(HttpStatus.FORBIDDEN, "COMMON_4031", "읽기 전용 권한만 있습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_4041", "잘못된 API 엔드포인트입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_4050", "요청된 HTTP 메서드가 허용되지 않습니다."),
    CONFLICT(HttpStatus.CONFLICT, "COMMON_4090", "현재 서버의 리소스 상태와 충돌이 발생했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_5000", "서버 내부 오류가 발생했습니다."),
    DB_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_5001", "데이터베이스 오류가 발생했습니다."),
    OBJECT_STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_5002", "오브젝트 스토리지 오류가 발생했습니다."),

    // 회원 서비스
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "MEMBER_4000", "기존 비밀번호와 일치하지 않습니다."),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "MEMBER_4001", "이메일 형식이 올바르지 않습니다."),
    INVALID_USERNAME(HttpStatus.BAD_REQUEST, "MEMBER_4002", "아이디 형식이 올바르지 않습니다."),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "MEMBER_4003", "비밀번호 형식이 올바르지 않습니다."),
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "MEMBER_4004", "비밀번호 확인이 일치하지 않습니다."),
    MISSING_USERNAME(HttpStatus.BAD_REQUEST, "MEMBER_4005", "아이디 값이 누락되었습니다."),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "MEMBER_4006", "잘못된 역할 값이 제공되었습니다."),
    INVALID_PAGE_NUMBER(HttpStatus.BAD_REQUEST, "MEMBER_4007", "유효하지 않은 페이지 번호입니다."),
    INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "MEMBER_4008", "유효하지 않은 페이지 크기입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "MEMBER_4010", "아이디 또는 비밀번호가 올바르지 않습니다."),
    PASSWORD_INCORRECT(HttpStatus.UNAUTHORIZED, "MEMBER_4011", "비밀번호가 올바르지 않습니다. 다시 시도해주세요."),
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "MEMBER_4012", "로그인이 필요한 요청입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_4040", "존재하지 않는 회원입니다."),
    EMAIL_ALREADY_USED(HttpStatus.CONFLICT, "MEMBER_4091", "이미 사용 중인 이메일 주소입니다."),

    // 템플릿 서비스
    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "TEMPLATE_4040", "존재하지 않는 템플릿입니다."),
    DUPLICATE_TEMPLATE(HttpStatus.CONFLICT, "TEMPLATE_4090", "동일한 제목의 템플릿이 이미 존재합니다."),

    // 티켓 서비스
    INVALID_TICKET_TITLE(HttpStatus.BAD_REQUEST, "TICKET_4000", "유효하지 않은 티켓 제목입니다."),
    INVALID_TICKET_DESCRIPTION(HttpStatus.BAD_REQUEST, "TICKET_4001", "유효하지 않은 티켓 설명입니다."),
    INVALID_TICKET_STATUS(HttpStatus.BAD_REQUEST, "TICKET_4003", "유효하지 않은 티켓 상태 값입니다."),
    TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "TICKET_4040", "존재하지 않는 티켓입니다."),

    // 댓글 서비스
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_4040", "존재하지 않는 댓글입니다."),

    // 카테고리 서비스
    MISSING_CATEGORY_NAME(HttpStatus.BAD_REQUEST, "CATEGORY_4000", "카테고리 이름이 누락되었습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_4040", "존재하지 않는 카테고리입니다."),
    DUPLICATE_CATEGORY(HttpStatus.CONFLICT, "CATEGORY_4090", "동일한 이름의 카테고리가 이미 존재합니다."),

    // 통계 서비스
    MISSING_DATE_RANGE(HttpStatus.BAD_REQUEST, "STATS_4000", "시작 날짜 또는 종료 날짜가 누락되었습니다."),
    INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "STATS_4001", "날짜 형식이 올바르지 않습니다."),
    STAT_NOT_FOUND(HttpStatus.NOT_FOUND, "STATS_4041", "존재하지 않는 통계 데이터입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}