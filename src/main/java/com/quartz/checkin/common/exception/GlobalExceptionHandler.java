package com.quartz.checkin.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import software.amazon.awssdk.core.exception.SdkClientException;


import java.util.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // 커스텀 예외 처리
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleCustomException(ApiException ex) {
        log.error("ApiException 발생: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex.getApiCode());
    }

    // @Valid 검증 예외 처리
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        log.warn("ValidationException 발생: {}", ex.getMessage());

        List<Map<String, String>> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            Map<String, String> error = new HashMap<>();
            error.put("field", fieldError.getField());
            error.put("message", fieldError.getDefaultMessage());
            errors.add(error);
        }

        return buildErrorResponse(ApiCode.INVALID_DATA, errors);
    }

    // @Validated 검증 예외 처리
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex) {
        log.warn("ConstraintViolationException 발생: {}", ex.getMessage());

        List<Map<String, String>> errors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            Map<String, String> error = new HashMap<>();
            error.put("field", violation.getPropertyPath().toString());
            error.put("message", violation.getMessage());
            errors.add(error);
        }

        return buildErrorResponse(ApiCode.INVALID_DATA, errors);
    }

    // API 인자 불일치 예외 처리
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("MethodArgumentTypeMismatchException 발생: {}", ex.getMessage());
        return buildErrorResponse(ApiCode.METHOD_NOT_ALLOWED);
    }

    // DB 유효성 예외 처리
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.error("DataIntegrityViolationException 발생: {}", ex.getMessage(), ex);
        return buildErrorResponse(ApiCode.DB_ERROR);
    }

    // S3 API 예외 처리
    @ExceptionHandler(SdkClientException.class)
    public ResponseEntity<Object> handleAwsServiceException(SdkClientException ex) {
        log.error("AWS SDK Exception 발생: {}", ex.getMessage(), ex);
        return buildErrorResponse(ApiCode.OBJECT_STORAGE_ERROR, ex.getMessage());
    }

    // 그 외 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException(Exception ex) {
        log.error("Unexpected Exception 발생: {}", ex.getMessage(), ex);
        return buildErrorResponse(ApiCode.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    // 공통 응답 빌더
    private ResponseEntity<Object> buildErrorResponse(ApiCode apiCode) {
        return ResponseEntity.status(apiCode.getHttpStatus()).body(ApiResponse.onFailure(apiCode));
    }

    private ResponseEntity<Object> buildErrorResponse(ApiCode apiCode, Object errors) {
        return ResponseEntity.status(apiCode.getHttpStatus()).body(ApiResponse.onFailure(apiCode, errors));
    }
}
