package com.quartz.checkin.security.handler;

import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.common.ServletResponseUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        log.warn("권한이 부족합니다.");
        ServletResponseUtils.writeApiErrorResponse(response, ErrorCode.FORBIDDEN);
    }
}
