package com.quartz.checkin.security.service;

import com.quartz.checkin.common.exception.InValidAccessTokenException;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Role;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.security.CustomUser;
import io.jsonwebtoken.Claims;

import java.time.LocalDateTime;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final JwtService jwtService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("{}의 정보를 DB로부터 읽어옵니다.", username);
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사용자입니다."));

        if (member.getDeletedAt() != null) {
            log.error("소프트 딜리트된 사용자가 로그인하려 합니다.");
            throw new UsernameNotFoundException("소프트 딜리트된 사용자입니다.");
        }


        return new CustomUser(
                member.getId(),
                member.getUsername(),
                member.getPassword(),
                member.getEmail(),
                member.getProfilePic(),
                member.getRole(),
                member.getPasswordChangedAt(),
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + member.getRole().getValue()))
        );
    }

    public UserDetails loadUserByAccessToken(String accessToken) throws InValidAccessTokenException {
        try {
            log.info("accessToken으로부터 사용자 정보를 읽어옵니다.");
            Claims claims = jwtService.decodeToken(accessToken);
            Role role = Role.fromValue(claims.get(JwtService.ROLE_CLAIM, String.class));

            return new CustomUser(
                    claims.get(JwtService.ID_CLAIM, Long.class),

                    claims.get(JwtService.USERNAME_CLAIM, String.class),
                    "",
                    "",
                    claims.get(JwtService.PROFILE_PIC_CLAIM, String.class),
                    role,
                    null,
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_" + role.getValue()))
            );
        } catch (Exception e) {
            log.error("사용자 정보를 읽어오는데 실패하였습니다. {}", e.getMessage());
            throw new InValidAccessTokenException();
        }
    }
}
