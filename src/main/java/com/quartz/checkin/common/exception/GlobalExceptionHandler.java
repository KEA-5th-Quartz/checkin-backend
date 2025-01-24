package com.quartz.checkin.common.exception;

import com.quartz.checkin.dto.response.ApiErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import software.amazon.awssdk.core.exception.SdkException;


import java.util.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // 커스텀 예외 처리
    @ExceptionHandler
    public ResponseEntity<Object> HandleCustomException(ApiException ex) {
        return handleExceptionInternal(ex.getErrorCode());
    }

    // @Valid 검증 예외 처리
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {

        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();

        List<Map<String, String>> errors = new ArrayList<>();
        for (FieldError fieldError : fieldErrors) {

            Map<String, String> error = new HashMap<>();
            error.put("field", fieldError.getField());
            error.put("message", fieldError.getDefaultMessage());

            errors.add(error);
        }

        return handleExceptionInternal(ErrorCode.INVALID_DATA, errors);
    }

    // @Validated 검증 예외 처리
    @ExceptionHandler
    protected ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex) {

        Set<ConstraintViolation<?>> fieldErrors = ex.getConstraintViolations();

        List<Map<String, String>> errors = new ArrayList<>();
        for (ConstraintViolation<?> fieldError : fieldErrors) {
            Map<String, String> error = new HashMap<>();

            String field = null;
            for (Path.Node node : fieldError.getPropertyPath()) {
                field = node.getName();
            }
            error.put("field", field);
            error.put("message", fieldError.getMessage());

            errors.add(error);
        }

        return handleExceptionInternal(ErrorCode.INVALID_DATA, errors);
    }

    // API 인자 불일치 예외 처리
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e) {
        return handleExceptionInternal(ErrorCode.METHOD_NOT_ALLOWED);
    }

    // DB 유효성 예외 처리
    @ExceptionHandler
    public ResponseEntity<Object> HandleDataIntegrityViolationException(DataIntegrityViolationException e) {
        return handleExceptionInternal(ErrorCode.DB_ERROR);
    }

    // S3 API 예외 처리
    @ExceptionHandler
    public ResponseEntity<Object> HandleAwsServiceException(SdkException e) {
        return handleExceptionInternal(ErrorCode.OBJECT_STORAGE_ERROR, e.getMessage());
    }

    // 그 외 모든 예외 처리
    @ExceptionHandler
    public ResponseEntity<Object> handleException(Exception e) {
        log.error(e.getMessage(), e);

        return handleExceptionInternal(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    private ResponseEntity<Object> handleExceptionInternal(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus()).body(ApiErrorResponse.createErrorResponse(errorCode));
    }

    private ResponseEntity<Object> handleExceptionInternal(ErrorCode errorCode, Object errors) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiErrorResponse.createErrorResponseWithData(errorCode, errors));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        log.error(ex.getMessage(), ex);

        String message;
        if (ex instanceof ErrorResponse errorResponse) {
            if (body == null) {
                message = errorResponse.updateAndGetBody(this.getMessageSource(), LocaleContextHolder.getLocale())
                        .getDetail();
            } else {
                message = ((ErrorResponse) body).getDetailMessageCode();
            }
        } else {
            message = ex.getMessage();
        }

        body = ApiErrorResponse.builder()
                .status(statusCode.value())
                .code("COMMON")
                .data(null)
                .message(message);

        return ResponseEntity.status(statusCode.value()).body(body);
    }

}