package com.quartz.checkin.security.handler;

import com.quartz.checkin.common.ServletRequestUtils;
import com.quartz.checkin.security.CustomUser;
import com.quartz.checkin.security.service.JwtService;
import com.quartz.checkin.service.MemberAccessLogService;
import com.quartz.checkin.service.MemberService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final MemberAccessLogService memberAccessLogService;
    private final MemberService memberService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        log.info("로그인에 성공하였습니다.");
        CustomUser user = (CustomUser) authentication.getPrincipal();
        jwtService.setAuthenticationResponse(response, user);

        String refreshToken = jwtService.createRefreshToken();
        jwtService.setRefreshToken(response, refreshToken);
        memberService.updateMemberRefreshToken(user.getId(), refreshToken);

        String clientIp = ServletRequestUtils.getClientIp(request);
        memberAccessLogService.writeLoginSuccessAccessLog(user.getId(), clientIp);

    }
}
