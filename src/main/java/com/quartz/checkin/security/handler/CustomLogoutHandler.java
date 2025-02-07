package com.quartz.checkin.security.handler;

import com.quartz.checkin.security.CustomUser;
import com.quartz.checkin.security.service.JwtService;
import com.quartz.checkin.service.MemberService;
import com.quartz.checkin.service.TokenBlackListCacheService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLogoutHandler implements LogoutHandler {

    private final JwtService jwtService;
    private final MemberService memberService;
    private final TokenBlackListCacheService tokenBlackListCacheService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        CustomUser customUser = (CustomUser) authentication.getPrincipal();
        log.info("{}가 로그아웃 합니다.", customUser.getUsername());

        String accessToken = jwtService.extractAccessTokenFromRequest(request).get();
        tokenBlackListCacheService.addBlacklist(accessToken);

        jwtService.expireRefreshTokenCookie(response);

        memberService.updateMemberRefreshToken(customUser.getId(), null);

        //TODO 로그아웃한 사용자의 accessToken 블랙리스트에 등록
    }
}
