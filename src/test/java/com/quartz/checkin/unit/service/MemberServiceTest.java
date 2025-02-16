package com.quartz.checkin.unit.service;


import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.member.request.MemberRegistrationRequest;
import com.quartz.checkin.dto.member.request.PasswordChangeRequest;
import com.quartz.checkin.dto.member.request.PasswordResetEmailRequest;
import com.quartz.checkin.dto.member.request.PasswordResetRequest;
import com.quartz.checkin.dto.member.request.RoleUpdateRequest;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Role;
import com.quartz.checkin.event.MemberRegisteredEvent;
import com.quartz.checkin.event.PasswordResetMailEvent;
import com.quartz.checkin.event.RoleUpdateEvent;
import com.quartz.checkin.event.SoftDeletedEvent;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.security.CustomUser;
import com.quartz.checkin.security.service.JwtService;
import com.quartz.checkin.service.MemberService;
import com.quartz.checkin.service.S3Service;
import java.io.IOException;
import java.util.Collections;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

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
    @Mock
    JwtService jwtService;
    @Mock
    S3Service s3Service;

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

    @Nested
    @DisplayName("회원 비밀번호 변경 테스트")
    class PasswordChangeTests {

        private Long id;
        private String username;
        private String originalPassword;
        private String newPassword;
        private Role role;
        private Member existingMember;
        private CustomUser customUser;
        private PasswordChangeRequest request;

        @BeforeEach
        void setUp() {
            id = 1L;
            username = "user.a";
            originalPassword = "originalPassword!";
            newPassword = "newPassword!";
            role = Role.USER;

            existingMember = Member.builder()
                    .id(id)
                    .password(originalPassword)
                    .build();

            customUser = new CustomUser(
                    id,
                    username,
                    originalPassword,
                    "email",
                    "profile",
                    role,
                    null,
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_" + role.getValue()))
            );

            request = new PasswordChangeRequest(originalPassword, newPassword);
        }


        @Test
        @DisplayName("비밀번호 변경 성공")
        public void passwordChangeSuccess() {
            //given
            when(memberRepository.findById(id)).thenReturn(Optional.of(existingMember));
            when(passwordEncoder.matches(originalPassword, existingMember.getPassword())).thenReturn(true);

            //when & then
            assertThatNoException().isThrownBy(() -> memberService.changeMemberPassword(id, customUser, request));
        }

        @Test
        @DisplayName("비밀번호 변경 실패 - 존재하지 않는 사용자")
        public void passwordChangeFailsUserDoesNotExist() {
            //given
            when(memberRepository.findById(any())).thenReturn(Optional.empty());

            //when & then
            assertThatThrownBy(() -> memberService.changeMemberPassword(2L, customUser, request))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.MEMBER_NOT_FOUND));
        }

        @Test
        @DisplayName("비밀번호 변경 실패 - 다른 사용자의 비밀번호를 변경")
        public void passwordChangeFailsTryingToChangeAnotherMembersPassword() {
            //given
            Member existingMember = Member.builder()
                    .id(2L)
                    .username("user.b")
                    .password("otherPassword!")
                    .build();

            when(memberRepository.findById(2L)).thenReturn(Optional.of(existingMember));

            //when & then
            assertThatThrownBy(() -> memberService.changeMemberPassword(2L, customUser, request))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.FORBIDDEN));
        }

        @Test
        @DisplayName("비밀번호 변경 실패 - 기존 비밀번호가 틀림")
        public void passwordChangeFailsWhenOriginalPasswordIsWrong() {
            //given
            String wrongOriginalPassword = "wrongOriginalPassword!";
            request = new PasswordChangeRequest(wrongOriginalPassword, newPassword);

            when(memberRepository.findById(id)).thenReturn(Optional.of(existingMember));
            when(passwordEncoder.matches(wrongOriginalPassword, customUser.getPassword())).thenReturn(false);

            //when & then
            assertThatThrownBy(() -> memberService.changeMemberPassword(id, customUser, request))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.INVALID_ORIGINAL_PASSWORD));
        }

        @Test
        @DisplayName("비밀번호 변경 실패 - 기존 비밀번호와 새 비밀번호가 같음")
        public void passwordChangeFailsWhenNewPasswordIsSameAsOriginal() {
            //given
            request = new PasswordChangeRequest(originalPassword, originalPassword);

            when(memberRepository.findById(id)).thenReturn(Optional.of(existingMember));
            when(passwordEncoder.matches(originalPassword, customUser.getPassword())).thenReturn(true);

            //when & then
            assertThatThrownBy(() -> memberService.changeMemberPassword(id, customUser, request))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.INVALID_NEW_PASSWORD));
        }
    }

    @Nested
    @DisplayName("비밀번호 초기화 테스트")
    class PasswordResetTests {

        private Long id;
        private String newPassword;
        private String passwordResetToken;
        private Member existingMember;
        private PasswordResetRequest request;

        @BeforeEach
        public void setUp() {
            id = 1L;
            newPassword = "newPassowrd!";
            passwordResetToken = "passwordResetToken";

            existingMember = Member.builder()
                    .id(id)
                    .password("originalPassword!")
                    .build();

            request = new PasswordResetRequest(passwordResetToken, newPassword);
        }

        @Test
        @DisplayName("비밀번호 초기화 성공")
        public void passwordResetSuccess() {
            //given
            String encodedNewPassword = "encodedNewPassword!";
            when(memberRepository.findById(id)).thenReturn(Optional.of(existingMember));
            when(jwtService.isValidToken(passwordResetToken)).thenReturn(true);
            when(jwtService.getMemberIdFromPasswordResetToken(passwordResetToken)).thenReturn(id);
            when(passwordEncoder.encode(request.getNewPassword())).thenReturn(encodedNewPassword);

            //when & then
            assertThatNoException().isThrownBy(() -> memberService.resetMemberPassword(id, request));
            assertThat(existingMember.getPassword()).isEqualTo(encodedNewPassword);
        }

        @Test
        @DisplayName("비밀번호 초기화 실패 - 존재하지 않는 사용자")
        public void passwordResetFailsWhenUserDoesNotExist() {
            //given
            when(memberRepository.findById(id)).thenReturn(Optional.empty());

            //when & then
            assertThatThrownBy((() -> memberService.resetMemberPassword(id, request)))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.MEMBER_NOT_FOUND));
        }

        @Test
        @DisplayName("비밀번호 초기화 실패 - 유효하지 않은 비밀번호 초기화 토큰")
        public void passwordResetFailsWhenPasswordResetTokenIsInvalid() {
            //given
            when(memberRepository.findById(id)).thenReturn(Optional.of(existingMember));
            when(jwtService.isValidToken(passwordResetToken)).thenReturn(false);

            //when & then
            assertThatThrownBy((() -> memberService.resetMemberPassword(id, request)))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));
        }

        @Test
        @DisplayName("비밀번호 초기화 실패 - 다른 사용자의 비밀번호 초기화 토큰")
        public void passwordResetFailsWhenUsingAnotherUsersToken() {
            //given
            when(memberRepository.findById(id)).thenReturn(Optional.of(existingMember));
            when(jwtService.isValidToken(passwordResetToken)).thenReturn(true);
            when(jwtService.getMemberIdFromPasswordResetToken(passwordResetToken)).thenReturn(2L);

            //when & then
            assertThatThrownBy(() -> memberService.resetMemberPassword(id, request))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("비밀번호 초기화 이메일 전송 테스트")
    class PasswordResetEmailTests {

        private Long id;
        private String username;
        private String passwordResetToken;
        private Member existingMember;
        private PasswordResetEmailRequest request;

        @BeforeEach
        public void setUp() {
            id = 1L;
            username = "user.a";
            passwordResetToken = "passwordResetToken";

            existingMember = Member.builder()
                    .id(id)
                    .username(username)
                    .build();

            request = new PasswordResetEmailRequest(username);
        }

        @Test
        @DisplayName("비밀번호 초기화 이메일 전송 성공")
        public void sendPasswordResetEmailSuccess() {
            //given
            when(memberRepository.findByUsername(username)).thenReturn(Optional.of(existingMember));
            when(jwtService.createPasswordResetToken(id)).thenReturn(passwordResetToken);

            //when
            memberService.sendPasswordResetMail(request);

            //then
            verify(eventPublisher).publishEvent(any(PasswordResetMailEvent.class));
        }

        @Test
        @DisplayName("비밀번호 초기화 이메일 전송 실패 - 존재하지 않는 사용자")
        public void sendPasswordResetEmailFailsWhenUserDoesNotExist() {
            //given
            when(memberRepository.findByUsername(username)).thenReturn(Optional.empty());

            //when & then
            assertThatThrownBy(() -> memberService.sendPasswordResetMail(request))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.MEMBER_NOT_FOUND));
        }

    }

    @Nested
    @DisplayName("프로필 사진 업데이트 테스트")
    class ProfilePicUpdateTests {

        private Long id;
        private MultipartFile file;
        private Member existingMember;
        private CustomUser customUser;

        @BeforeEach
        public void setUp() {
            id = 1L;
            file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());

            existingMember = Member.builder()
                    .id(id)
                    .build();

            customUser = new CustomUser(
                    id,
                    "user.a",
                    "password",
                    "email",
                    "profilePic",
                    Role.USER,
                    null,
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_" + Role.USER.getValue()))
            );
        }

        @Test
        @DisplayName("프로필 사진 업데이트 성공")
        public void profilePicUpdateSuccess() throws IOException {
            //given
            when(s3Service.isImageType(file.getContentType())).thenReturn(true);
            when(memberRepository.findById(id)).thenReturn(Optional.of(existingMember));
            when(s3Service.uploadFile(eq(file), anyString())).thenReturn("newProfilePic");

            //when & then
            assertThatNoException().isThrownBy(() -> memberService.updateMemberProfilePic(id, customUser, file));
            assertThat(existingMember.getProfilePic()).isEqualTo("newProfilePic");
        }

        @Test
        @DisplayName("프로필 사진 업데이트 실패 - 이미지 파일이 아닌 경우")
        public void profilePicUpdateFailsWhenFileIsNotImageType() {
            //given
            when(s3Service.isImageType(file.getContentType())).thenReturn(false);

            //when & then
            assertThatThrownBy(() -> memberService.updateMemberProfilePic(id, customUser, file))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.INVALID_DATA));
        }

        @Test
        @DisplayName("프로필 사진 업데이트 실패 - 이미지 파일이 5MB를 넘어가는 경우")
        public void profilePicUpdateFailsWhenFileIsExceedsSizeLimit() {
            //given
            byte[] fileContent = new byte[5 * 1024 * 1024 + 1];
            file = new MockMultipartFile("file", "test.jpg", "image/jpeg", fileContent);
            when(s3Service.isImageType(file.getContentType())).thenReturn(true);

            //when & then
            assertThatThrownBy(() -> memberService.updateMemberProfilePic(id, customUser, file))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.TOO_LARGE_FILE));
        }

        @Test
        @DisplayName("프로필 사진 업데이트 실패 - 존재하지 않는 사용자")
        public void profilePicUpdateFailsWhenUserDoesNotExist() {
            //given
            when(s3Service.isImageType(file.getContentType())).thenReturn(true);
            when(memberRepository.findById(id)).thenReturn(Optional.empty());

            //when & then
            assertThatThrownBy(() -> memberService.updateMemberProfilePic(id, customUser, file))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.MEMBER_NOT_FOUND));
        }

        @Test
        @DisplayName("프로필 사진 업데이트 실패 - 다른 사용자의 프로필을 업데이트")
        public void profilePicUpdateFailsWhenUpdatingAnotherUsersProfile() {
            //given
            customUser = new CustomUser(
                    2L,
                    "user.b",
                    "password",
                    "email",
                    "profilePic",
                    Role.USER,
                    null,
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_" + Role.USER.getValue()))

            );
            when(s3Service.isImageType(file.getContentType())).thenReturn(true);
            when(memberRepository.findById(id)).thenReturn(Optional.of(existingMember));

            //when & then
            assertThatThrownBy(() -> memberService.updateMemberProfilePic(id, customUser, file))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.FORBIDDEN));
        }

        @Test
        @DisplayName("프로필 사진 업데이트 실패 - ObjectStorage 클라이언트 문제 발생")
        public void profilePicUpdateFailsWhenObjectStorageClientErrorOccurs() throws IOException {
            //given
            when(s3Service.isImageType(file.getContentType())).thenReturn(true);
            when(memberRepository.findById(id)).thenReturn(Optional.of(existingMember));
            when(s3Service.uploadFile(eq(file), anyString())).thenThrow(new RuntimeException());

            //when & then
            assertThatThrownBy(() -> memberService.updateMemberProfilePic(id, customUser, file))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.OBJECT_STORAGE_ERROR));
        }

    }

    @Nested
    @DisplayName("권한 변경 테스트")
    class RoleUpdateTests {

        private Long id;
        private Role prevRole;
        private String newRole;
        private Member existingMember;
        private RoleUpdateRequest request;

        @BeforeEach
        public void setUp() {
            id = 1L;
            prevRole = Role.USER;
            newRole = "MANAGER";

            existingMember = Member.builder()
                    .id(id)
                    .role(prevRole)
                    .build();

            request = new RoleUpdateRequest(newRole);
        }

        @Test
        @DisplayName("권한 변경 성공")
        public void RoleUpdateSuccess() {
            //given
            when(memberRepository.findById(id)).thenReturn(Optional.of(existingMember));

            //when
            memberService.updateMemberRole(id, request);

            //then
            assertThat(existingMember.getRole().getValue()).isEqualTo(newRole);
            verify(eventPublisher).publishEvent(any(RoleUpdateEvent.class));
        }

        @Test
        @DisplayName("권한 변경 실패 - 존재하지 않는 사용자")
        public void RoleUpdateFailsWhenUserDoesNotExist() {
            //given
            when(memberRepository.findById(id)).thenReturn(Optional.empty());

            //when & then
            assertThatThrownBy(() -> memberService.updateMemberRole(id, request))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.MEMBER_NOT_FOUND));
        }

        @Test
        @DisplayName("권한 변경 실패 - 기존 권한과 동일함")
        public void RoleUpdateFailsWhenWhenRoleIsUnchanged() {
            //given
            when(memberRepository.findById(id)).thenReturn(Optional.of(existingMember));
            newRole = "USER";
            request = new RoleUpdateRequest(newRole);

            //when & then
            assertThatThrownBy(() -> memberService.updateMemberRole(id, request))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.INVALID_NEW_ROLE));
        }

    }

    @Nested
    @DisplayName("소프트 딜리트 테스트")
    class SoftDeleteTests {

        private Long id;
        private Member existingMember;

        @BeforeEach
        public void setUp() {
            id = 1L;
            existingMember = Member.builder()
                    .id(id)
                    .build();
        }

        @Test
        @DisplayName("소프트 딜리트 성공")
        public void softDeleteSuccess() {
            //given
            when(memberRepository.findById(id)).thenReturn(Optional.of(existingMember));

            //when
            memberService.softDeleteMember(id);

            //then
            assertThat(existingMember.getDeletedAt()).isNotNull();
            verify(eventPublisher).publishEvent(any(SoftDeletedEvent.class));
        }

        @Test
        @DisplayName("소프트 딜리트 실패 - 존재하지 않는 사용자")
        public void softDeleteFailsWhenUserDoesNotExist() {
            //given
            when(memberRepository.findById(id)).thenReturn(Optional.empty());

            //when & then
            assertThatThrownBy(() -> memberService.softDeleteMember(id))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.MEMBER_NOT_FOUND));
        }

        @Test
        @DisplayName("소프트 딜리트 실패 - 이미 소프트 딜리트된 사용자")
        public void softDeleteFailsWhenUserIsAlreadySoftDeleted() {
            //given
            existingMember.softDelete();
            when(memberRepository.findById(id)).thenReturn(Optional.of(existingMember));

            //when & then
            assertThatThrownBy(() -> memberService.softDeleteMember(id))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.MEMBER_ALREADY_SOFT_DELETED));
        }
    }

}
