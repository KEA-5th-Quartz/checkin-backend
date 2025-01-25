package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.request.MemberRegistrationRequest;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.common.PasswordGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public Member getMemberByIdOrThrow(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 사용자입니다.");
                    return new ApiException(ErrorCode.MEMBER_NOT_FOUND);
                });
    }

    @Transactional
    public void register(MemberRegistrationRequest memberRegistrationRequest) {
        String username = memberRegistrationRequest.getUsername();
        String email = memberRegistrationRequest.getEmail();

        checkUsernameDuplicate(username);

        checkEmailDuplicate(email);

        String password = passwordEncoder.encode(PasswordGenerator.generateRandomPassword());
         memberRepository.save(Member.from(memberRegistrationRequest, password));

    }

    @Transactional
    public void updateMemberRefreshToken(Long id, String refreshToken) {
        Member member = getMemberByIdOrThrow(id);

        member.updateRefreshToken(refreshToken);
    }

    private void checkEmailDuplicate(String email) {
        if (memberRepository.findByEmail(email).isPresent()) {
            log.error("사용중인 이메일입니다. {}", email);
            throw new ApiException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    private void checkUsernameDuplicate(String username) {
        if (memberRepository.findByUsername(username).isPresent()) {
            log.error("사용중인 사용자 이름입니다. {}", username);
            throw new ApiException(ErrorCode.DUPLICATE_USERNAME);
        }
    }

}
