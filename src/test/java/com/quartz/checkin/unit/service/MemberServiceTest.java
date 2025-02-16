package com.quartz.checkin.unit.service;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.member.request.MemberRegistrationRequest;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.event.MemberRegisteredEvent;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.service.MemberService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {

    @InjectMocks
    MemberService memberService;
    @Mock
    MemberRepository memberRepository;
    @Mock
    ApplicationEventPublisher eventPublisher;
    @Mock
    PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("회원 생성 테스트")
    class MemberRegisterTests {

        private String username;
        private String email;
        private String role;
        private Member existingMember;
        private MemberRegistrationRequest request;

        @BeforeEach
        void setUp() {
            username = "new.user";
            email = "newUser@email.com";
            role = "USER";

            existingMember = Member.builder()
                    .id(1L)
                    .email(email)
                    .build();

            request = new MemberRegistrationRequest(username, email, role);
        }

        @Test
        @DisplayName("회원 생성 성공")
        public void memberRegisterSuccess() {
            //given
            when(memberRepository.findByUsername(username)).thenReturn(Optional.empty());
            when(memberRepository.findByEmail(email)).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

            //when & then
            assertThatNoException().isThrownBy(() -> memberService.register(request));
            verify(memberRepository).save(any(Member.class));
            verify(eventPublisher).publishEvent(any(MemberRegisteredEvent.class));
        }

        @Test
        @DisplayName("회원 생성 실패 - 사용자명 중복")
        public void memberRegisterFailsWhenUsernameAlreadyExists() {
            //given
            when(memberRepository.findByUsername(username)).thenReturn(Optional.of(existingMember));

            //when & then
            assertThatThrownBy(() -> memberService.register(request))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.DUPLICATE_USERNAME));
        }

        @Test
        @DisplayName("회원 생성 실패 - 이메일 중복")
        public void memberRegisterFailsWhenUsernameEmailExists() {
            //given
            when(memberRepository.findByUsername(username)).thenReturn(Optional.empty());
            when(memberRepository.findByEmail(email)).thenReturn(Optional.of(existingMember));

            //when & then
            assertThatThrownBy(() -> memberService.register(request))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.DUPLICATE_EMAIL));
        }

    }



}
