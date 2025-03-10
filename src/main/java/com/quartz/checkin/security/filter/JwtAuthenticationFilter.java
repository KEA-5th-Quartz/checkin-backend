package com.quartz.checkin.security.filter;

import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.common.exception.InValidAccessTokenException;
import com.quartz.checkin.security.service.CustomUserDetailsService;
import com.quartz.checkin.security.service.JwtService;
import com.quartz.checkin.common.ServletResponseUtils;
import com.quartz.checkin.service.RoleUpdateCacheService;
import com.quartz.checkin.service.SoftDeletedMemberCacheService;
import com.quartz.checkin.service.TokenBlackListCacheService;
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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final AntPathRequestMatcher[] SKIP_PATHS = {
            new AntPathRequestMatcher("/auth/login"),
            new AntPathRequestMatcher("/auth/refresh"),
            new AntPathRequestMatcher("/swagger-ui/**"),
            new AntPathRequestMatcher("/v3/api-docs/**"),
            new AntPathRequestMatcher("/swagger-ui.html"),
            new AntPathRequestMatcher("/members/check-*"),
            new AntPathRequestMatcher("/members/password-reset"),
            new AntPathRequestMatcher("/members/*/password-reset")
    };

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final TokenBlackListCacheService tokenBlackListCacheService;
    private final RoleUpdateCacheService roleUpdateCacheService;
    private final SoftDeletedMemberCacheService softDeletedMemberCacheService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException, AuthenticationException {

        String requestURI = request.getRequestURI();

        if (isSkipPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.info("요청({})에 대한 JWT 인증 필터를 시작합니다.", requestURI);

        String accessToken = jwtService.extractAccessTokenFromRequest(request).orElse(null);

        if (accessToken == null) {
            log.warn("accessToken이 누락되었습니다.");
            ServletResponseUtils.writeApiErrorResponse(response, ErrorCode.UNAUTHENTICATED);
            return;
        }

        if (!jwtService.isValidToken(accessToken)) {
            throw new InValidAccessTokenException();
        }

        if (tokenBlackListCacheService.isBlackList(accessToken)) {
            log.error("블랙리스트에 속한 토큰으로 접근하려 합니다.");
            throw new InValidAccessTokenException();
        }

        UserDetails userDetails = customUserDetailsService.loadUserByAccessToken(accessToken);
        if (roleUpdateCacheService.isRoleUpdated(userDetails.getUsername())) {
            log.error("권한 변경 기록이 있는 사용자입니다. 재발급이 필요합니다.");
            throw new InValidAccessTokenException();
        }

        if (softDeletedMemberCacheService.isSoftDeleted(userDetails.getUsername())) {
            log.error("소프트 딜리트된 사용자입니다. 인증을 거부합니다.");
            throw new InValidAccessTokenException();
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
                userDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private boolean isSkipPath(HttpServletRequest request) {
        for (AntPathRequestMatcher matcher : SKIP_PATHS) {
            if (matcher.matches(request)) {
                return true;
            }
        }
        return false;
    }

}
