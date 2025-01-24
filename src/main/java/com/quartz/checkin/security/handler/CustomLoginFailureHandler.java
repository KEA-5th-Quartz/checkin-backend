package com.quartz.checkin.security.handler;

import com.quartz.checkin.exception.ErrorCode;
import com.quartz.checkin.utils.ServletResponseUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomLoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        if (exception instanceof UsernameNotFoundException) {
            log.warn("존재하지 않는 사용자입니다.");

        } else if (exception instanceof BadCredentialsException) {
            log.warn("비밀번호가 틀렸습니다.");
            //TODO 로그인 실패 횟수 카운트
        }

        ServletResponseUtils.writeErrorResponse(response, ErrorCode.WRONG_USERNAME_OR_PASSWORD);
    }
}
