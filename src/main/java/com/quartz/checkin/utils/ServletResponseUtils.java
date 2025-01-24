package com.quartz.checkin.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quartz.checkin.dto.response.ApiResponse;
import com.quartz.checkin.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.springframework.http.HttpStatus;

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

    public static void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) {
        response.setStatus(errorCode.getStatus());
        write(response, ErrorCode.toResponse(errorCode));
    }

    private static void write(HttpServletResponse response, Object value) {
        response.setContentType(ContentType.APPLICATION_JSON.getMimeType());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            response.getWriter().write(objectMapper.writeValueAsString(value));
        } catch (Exception e) {
            log.error("응답 바디를 적는 데 실패하였습니다. {}", e.getMessage());
        }
    }


}
