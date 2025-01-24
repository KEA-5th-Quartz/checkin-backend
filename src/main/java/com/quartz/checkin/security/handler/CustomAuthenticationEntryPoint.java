package com.quartz.checkin.security.handler;

import com.quartz.checkin.exception.ErrorCode;
import com.quartz.checkin.utils.ServletResponseUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        log.warn("토큰 인증에 실패하였습니다.");
        ServletResponseUtils.writeErrorResponse(response, ErrorCode.INVALID_ACCESS_TOKEN);

    }
}
