package com.quartz.checkin.security.filter;

import com.quartz.checkin.exception.ErrorCode;
import com.quartz.checkin.exception.custom.InvalidAccessTokenException;
import com.quartz.checkin.security.service.CustomUserDetailsService;
import com.quartz.checkin.security.service.JwtService;
import com.quartz.checkin.utils.ServletResponseUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String LOGIN_URL = "/auth/login";
    private static final String REFRESH_URL = "/auth/refresh";

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException, AuthenticationException {

        String requestURI = request.getRequestURI();

        if (requestURI.equals(LOGIN_URL) || requestURI.equals(REFRESH_URL)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.info("요청({})에 대한 JWT 인증 필터를 시작합니다.", requestURI);

        String accessToken = jwtService.extractAccessTokenFromRequest(request).orElse(null);

        if (accessToken == null) {
            log.warn("accessToken이 누락되었습니다.");
            ServletResponseUtils.writeErrorResponse(response, ErrorCode.UNAUTHENTICATED_ACCESS);
            return;
        }

        if (!jwtService.isValidToken(accessToken)) {
            throw new InvalidAccessTokenException("");
        }

        UserDetails userDetails = customUserDetailsService.loadUserByAccessToken(accessToken);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
