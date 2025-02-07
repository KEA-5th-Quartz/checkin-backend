package com.quartz.checkin.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quartz.checkin.common.ServletRequestUtils;
import com.quartz.checkin.common.ServletResponseUtils;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.auth.response.LoginBlockTimeLeftResponse;
import com.quartz.checkin.service.LoginBlockCacheService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StreamUtils;

@Slf4j
public class CustomUsernamePasswordAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private static final String DEFAULT_LOGIN_REQUEST_URL = "/auth/login";
    private static final String HTTP_METHOD = "POST";
    private static final String CONTENT_TYPE = "application/json";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private final ObjectMapper objectMapper;
    private final LoginBlockCacheService loginBlockCacheService;
    private static final AntPathRequestMatcher DEFAULT_LOGIN_PATH_REQUEST_MATCHER =
            new AntPathRequestMatcher(DEFAULT_LOGIN_REQUEST_URL, HTTP_METHOD);

    public CustomUsernamePasswordAuthenticationFilter(
            ObjectMapper objectMapper, LoginBlockCacheService loginBlockCacheService) {
        super(DEFAULT_LOGIN_PATH_REQUEST_MATCHER);
        this.objectMapper = objectMapper;
        this.loginBlockCacheService = loginBlockCacheService;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {
        if (request.getContentType() == null || !request.getContentType().equals(CONTENT_TYPE)) {
            throw new AuthenticationServiceException(
                    "Authentication Content-Type not supported: " + request.getContentType());
        }

        String messageBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);

        Map<String, String> usernamePasswordMap = objectMapper.readValue(messageBody, Map.class);

        String username = usernamePasswordMap.get(USERNAME);
        String password = usernamePasswordMap.get(PASSWORD);
        String ip = ServletRequestUtils.getClientIp(request);

        if (username == null || password == null) {
            log.warn("username 또는 password 값을 찾을 수 없습니다.");
            ServletResponseUtils.writeApiErrorResponse(response, ErrorCode.INVALID_DATA);
            return null;
        }

        String key = ip + ":" + username;
        log.info("{}가 로그인을 시도합니다.", key);

        if (loginBlockCacheService.isBlockedMember(key)) {
            long blockTimeLeft = loginBlockCacheService.getBlockTimeLeft(key) / 1000;
            String blockTime = String.format("%d분 %d초", blockTimeLeft / 60, blockTimeLeft % 60);
            log.error("로그인이 잠긴 ip와 아이디입니다. 남은 시간 {}", blockTime);

            LoginBlockTimeLeftResponse data = new LoginBlockTimeLeftResponse(blockTime);
            ServletResponseUtils.writeApiErrorResponseWithData(response, ErrorCode.BLOCKED_MEMBER, data);
            return null;
        }

        // 로그인 실패 핸들러에서 사용자 정보를 얻기 위해 저장
        request.setAttribute("username", username);

        UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username,
                password);

        return this.getAuthenticationManager().authenticate(authRequest);
    }
}
