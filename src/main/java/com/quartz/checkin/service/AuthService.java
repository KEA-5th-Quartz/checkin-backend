package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.response.AuthenticationResponse;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Role;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.security.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final JwtService jwtService;
    private final MemberRepository memberRepository;


    @Transactional
    public AuthenticationResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        log.info("토큰 재발급을 시도합니다.");
        String refreshToken = jwtService.extractRefreshToken(request).orElse(null);

        if (refreshToken == null || !jwtService.isValidToken(refreshToken)) {
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Member member = memberRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> {
                    log.error("해당 refreshToken을 소유한 사용자는 없습니다.");
                    return new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
                });

        Long memberId = member.getId();
        String username = member.getUsername();
        String profilePic = member.getProfilePic();
        Role role = member.getRole();
        LocalDateTime passwordChangedAt = member.getPasswordChangedAt();

        String accessToken = jwtService.createAccessToken(memberId, username, profilePic, role, passwordChangedAt);
        refreshToken = jwtService.createRefreshToken();

        member.updateRefreshToken(refreshToken);
        jwtService.setRefreshToken(response, refreshToken);

        return AuthenticationResponse.builder()
                .memberId(memberId)
                .username(username)
                .role(role.getValue())
                .profilePic(profilePic)
                .passwordChangedAt(passwordChangedAt)
                .accessToken(accessToken)
                .build();
    }
}
