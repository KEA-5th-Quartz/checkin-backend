package com.quartz.checkin.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.response.ApiErrorResponse;
import com.quartz.checkin.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@Slf4j
public class ServletResponseUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void writeApiResponse(HttpServletResponse response) {
        write(response, ApiResponse.createSuccessResponse(HttpStatus.OK.value()));
    }

    public static void writeApiResponseWithData(HttpServletResponse response, Object value) {
        ApiResponse<Object> apiResponse = ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), value);
        write(response, apiResponse);
    }

    public static void writeApiErrorResponse(HttpServletResponse response, ErrorCode errorCode) {
        response.setStatus(errorCode.getStatus().value());
        write(response, ApiErrorResponse.createErrorResponse(errorCode));
    }

    private static void write(HttpServletResponse response, Object value) {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            response.getWriter().write(objectMapper.writeValueAsString(value));
        } catch (Exception e) {
            log.error("응답 바디를 적는 데 실패하였습니다. {}", e.getMessage());
        }
    }


}
