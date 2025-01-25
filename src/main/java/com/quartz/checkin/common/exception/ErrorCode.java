package com.quartz.checkin.common.exception;

import com.quartz.checkin.dto.response.ApiErrorResponse;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 공통 예외
    INVALID_DATA(HttpStatus.BAD_REQUEST, "COMMON_4000", "필수로 요구되는 데이터가 비어있거나 규칙에 맞지 않습니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "COMMON_4010", "유효하지 않거나 만료된 accessToken입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "COMMON_4011", "유효하지 않거나 만료된 refreshToken입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_4030", "접근 권한이 없습니다."),
    UNAUTHENTICATED(HttpStatus.FORBIDDEN, "COMMON_4031", "인증이 필요한 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_4041", "잘못된 API 엔드포인트입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_4050", "요청된 HTTP 메서드가 허용되지 않습니다."),
    CONFLICT(HttpStatus.CONFLICT, "COMMON_4090", "현재 서버의 리소스 상태와 충돌이 발생했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_5000", "서버 내부 오류가 발생했습니다."),
    DB_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_5001", "데이터베이스 오류가 발생했습니다."),
    OBJECT_STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_5002", "오브젝트 스토리지 오류가 발생했습니다."),

    // 회원 서비스 예외
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "MEMBER_4003", "비밀번호 형식이 올바르지 않습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "MEMBER_4004", "비밀번호가 일치하지 않습니다."),
    USERNAME_MISSING(HttpStatus.BAD_REQUEST, "MEMBER_4005", "아이디 값이 누락되었습니다."),
    INVALID_PAGE_NUMBER(HttpStatus.BAD_REQUEST, "MEMBER_4007", "페이지 번호가 유효하지 않습니다."),
    INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "MEMBER_4008", "페이지 크기가 유효하지 않습니다."),
    INVALID_USERNAME_OR_PASSWORD(HttpStatus.UNAUTHORIZED, "MEMBER_4013", "일치하는 회원 정보가 없습니다. 아이디 혹은 비밀번호를 다시 확인해주세요."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.UNAUTHORIZED, "MEMBER_4015", "지원하지 않는 파일 형식입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_4040", "존재하지 않는 회원입니다."),
    NOT_REGISTERED(HttpStatus.NOT_FOUND, "MEMBER_4041", "가입하지 않은 아이디입니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "MEMBER_4090", "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "MEMBER_4091", "이미 사용 중인 이메일 주소입니다."),
    ADMIN_PERMISSION_REQUIRED(HttpStatus.CONFLICT, "MEMBER_4093", "관리자 권한이 없는 회원에게 최고 관리자 권한을 줄 수 없습니다."),

    // 템플릿 서비스 예외
    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "TEMPLATE_4040", "존재하지 않는 템플릿입니다."),
    DUPLICATE_TEMPLATE_TITLE(HttpStatus.CONFLICT, "TEMPLATE_4090", "동일한 제목을 가진 템플릿이 이미 존재합니다."),

    // 티켓 서비스 예외
    INVALID_TICKET_TITLE(HttpStatus.BAD_REQUEST, "TICKET_4000", "제목이 유효하지 않습니다."),
    INVALID_TICKET_DESCRIPTION(HttpStatus.BAD_REQUEST, "TICKET_4001", "설명이 유효하지 않습니다."),
    INVALID_TICKET_DUE_DATE(HttpStatus.BAD_REQUEST, "TICKET_4002", "마감 기한이 유효하지 않습니다."),
    INVALID_TICKET_STATUS(HttpStatus.BAD_REQUEST, "TICKET_4003", "티켓 상태 값이 유효하지 않습니다."),
    INVALID_TICKET_PRIORITY(HttpStatus.BAD_REQUEST, "TICKET_4004", "중요도 값이 유효하지 않습니다."),
    INVALID_TICKET_CATEGORY(HttpStatus.BAD_REQUEST, "TICKET_4005", "카테고리 값이 유효하지 않습니다."),
    INVALID_TICKET_MANAGER(HttpStatus.BAD_REQUEST, "TICKET_4006", "담당자 정보가 유효하지 않습니다."),
    INVALID_TICKET_PAGE_NUMBER(HttpStatus.BAD_REQUEST, "TICKET_4007", "페이지 번호가 유효하지 않습니다."),
    INVALID_TICKET_PAGE_SIZE(HttpStatus.BAD_REQUEST, "TICKET_4008", "페이지 크기가 유효하지 않습니다."),
    CANNOT_CHANGE_COMPLETED_TICKET(HttpStatus.BAD_REQUEST, "TICKET_4009", "이미 완료된 티켓은 상태를 변경할 수 없습니다."),
    UNSUPPORTED_TICKET_FILE_TYPE(HttpStatus.UNAUTHORIZED, "TICKET_4015", "지원하지 않는 파일 형식입니다."),
    TICKET_FILE_TOO_LARGE(HttpStatus.UNAUTHORIZED, "TICKET_4016", "파일 크기가 초과되었습니다."),
    TICKET_FILE_NOT_FOUND(HttpStatus.UNAUTHORIZED, "TICKET_4017", "파일이 존재하지 않습니다."),
    TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "TICKET_4040", "존재하지 않는 티켓입니다."),
    TICKET_KEYWORD_NOT_FOUND(HttpStatus.NOT_FOUND, "TICKET_4041", "해당 키워드를 가진 티켓이 존재하지 않습니다."),
    TICKET_ALREADY_ASSIGNED_TO_SELF(HttpStatus.CONFLICT, "TICKET_4091", "이미 본인이 담당자로 할당된 상태입니다."),

    // 댓글 서비스 예외
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_4040", "존재하지 않는 댓글입니다."),

    // 카테고리 서비스 예외
    CATEGORY_NAME_MISSING(HttpStatus.BAD_REQUEST, "CATEGORY_4000", "카테고리 이름이 누락되었습니다."),
    INVALID_CATEGORY_NAME_FORMAT(HttpStatus.BAD_REQUEST, "CATEGORY_4001", "카테고리 이름 형식이 올바르지 않습니다."),
    CATEGORY_NOT_FOUND_FIRST(HttpStatus.NOT_FOUND, "CATEGORY_4040", "존재하지 않는 1차 카테고리입니다."),
    CATEGORY_NOT_FOUND_SECOND(HttpStatus.NOT_FOUND, "CATEGORY_4041", "존재하지 않는 2차 카테고리입니다."),
    DUPLICATE_CATEGORY_FIRST(HttpStatus.CONFLICT, "CATEGORY_4090", "동일한 이름의 1차 카테고리가 이미 존재합니다."),
    DUPLICATE_CATEGORY_SECOND(HttpStatus.CONFLICT, "CATEGORY_4091", "동일한 이름의 2차 카테고리가 이미 존재합니다."),
    CATEGORY_HAS_SUBCATEGORIES(HttpStatus.CONFLICT, "CATEGORY_4092", "1차 카테고리에 속한 2차 카테고리가 존재하여 삭제할 수 없습니다."),

    // 통계 서비스 예외
    STATS_MISSING_DATE(HttpStatus.BAD_REQUEST, "STATS_4000", "시작 날짜 또는 종료 날짜가 누락되었습니다."),
    INVALID_STATS_DATE_FORMAT(HttpStatus.BAD_REQUEST, "STATS_4001", "날짜 형식이 올바르지 않습니다."),
    INVALID_STATS_DATE_RANGE(HttpStatus.BAD_REQUEST, "STATS_4002", "시작 날짜가 종료 날짜보다 큽니다."),
    STATS_MANAGER_NOT_FOUND(HttpStatus.NOT_FOUND, "STATS_4041", "존재하지 않는 담당자입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.status= httpStatus;
        this.code = code;
        this.message = message;
    }

}