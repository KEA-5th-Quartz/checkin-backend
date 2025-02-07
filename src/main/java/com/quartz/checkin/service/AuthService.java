package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.auth.response.AuthenticationResponse;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Role;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.security.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final JwtService jwtService;
    private final MemberRepository memberRepository;
    private final RoleUpdateCacheService roleUpdateCacheService;


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

        if (member.getDeleted_at() != null) {
            log.error("소프트 딜리트된 사용자가 재발급하려고 합니다.");
            throw new ApiException(ErrorCode.MEMBER_NOT_FOUND);
        }

        Long memberId = member.getId();
        String username = member.getUsername();
        String profilePic = member.getProfilePic();
        Role role = member.getRole();
        LocalDateTime passwordChangedAt = member.getPasswordChangedAt();

        if (roleUpdateCacheService.isRoleUpdated(username)) {
            log.info("권한 변경 기록이 있는 사용자 {}가 새로 로그인했습니다. 권한 변경 기록을 삭제합니다.", username);
            roleUpdateCacheService.evict(username);
        }

        String accessToken = jwtService.createAccessToken(memberId, username, profilePic, role);
        refreshToken = jwtService.createRefreshToken();
        String passwordResetToken =
                passwordChangedAt == null ? jwtService.createPasswordResetToken(memberId) : null;

        member.updateRefreshToken(refreshToken);
        jwtService.setRefreshToken(response, refreshToken);

        return AuthenticationResponse.builder()
                .memberId(memberId)
                .username(username)
                .role(role.getValue())
                .profilePic(profilePic)
                .passwordResetToken(passwordResetToken)
                .accessToken(accessToken)
                .build();
    }
}
