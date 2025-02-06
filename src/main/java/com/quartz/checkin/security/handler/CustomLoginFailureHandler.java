package com.quartz.checkin.security.handler;

import com.quartz.checkin.common.ServletRequestUtils;
import com.quartz.checkin.common.cache.LoginFailureInfo;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.common.ServletResponseUtils;
import com.quartz.checkin.service.LoginFailureCacheService;
import com.quartz.checkin.service.MemberAccessLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLoginFailureHandler implements AuthenticationFailureHandler {

    private final MemberAccessLogService memberAccessLogService;
    private final LoginFailureCacheService loginFailureCacheService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        if (exception instanceof UsernameNotFoundException) {
            log.warn("존재하지 않는 사용자입니다.");

        } else if (exception instanceof BadCredentialsException) {
            log.warn("비밀번호가 틀렸습니다.");

            String username = (String) request.getAttribute("username");
            String clientIp = ServletRequestUtils.getClientIp(request);
            memberAccessLogService.writeWrongPasswordAccessLog(username, clientIp);

            String key = clientIp + ":" + username;
            LoginFailureInfo loginFailureInfo = loginFailureCacheService.getLoginFailureInfo(key);

            if (loginFailureInfo.getCount() == 5) {
                log.error("사용자 로그인 잠금 필요");
                loginFailureCacheService.evictLoginFailureInfo(key);
            }
        }

        ServletResponseUtils.writeApiErrorResponse(response, ErrorCode.INVALID_USERNAME_OR_PASSWORD);
    }
}
