package com.quartz.checkin.integration;

import static com.quartz.checkin.util.ApiResponseMatchers.*;
import static com.quartz.checkin.util.ErrorResponseMatchers.*;
import static com.quartz.checkin.util.RequestAuthPostProcessor.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.quartz.checkin.event.SoftDeletedEvent;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.security.service.JwtService;
import com.quartz.checkin.service.LoginBlockCacheService;
import com.quartz.checkin.service.LoginFailureCacheService;
import com.quartz.checkin.service.MemberAccessLogService;
import com.quartz.checkin.service.S3Service;
import com.quartz.checkin.service.TokenBlackListCacheService;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles(value = "test")
@AutoConfigureMockMvc
@SpringBootTest
@Sql(scripts = "classpath:data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_CLASS)
@Transactional
public class MemberIntegrationTest {

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Autowired
    JwtService jwtService;

    @Autowired
    LoginBlockCacheService loginBlockCacheService;

    @Autowired
    LoginFailureCacheService loginFailureCacheService;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MemberAccessLogService memberAccessLogService;
    @Autowired
    MemberRepository memberRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PasswordEncoder passwordEncoder;

    @MockitoBean
    S3Service s3Service;

    @Autowired
    TokenBlackListCacheService tokenBlackListCacheService;

    @Value("${user.profile.defaultImageUrl}")
    private String profilePic;

    @TestConfiguration
    static class MockitoPublisherConfiguration {
        @Bean
        @Primary
        ApplicationEventPublisher publisher() {
            return mock(ApplicationEventPublisher.class);
        }
    }

    private String username;
    private String password;
    private String email;
    private String originalPassword;
    private String newPassword;
    private Role role;
    private Member savedMember;

    @BeforeEach
    public void setUp() {
        username = "test.user";
        password = "testPassword1@";
        email = "testUser@email.com";
        originalPassword = "originalPassword1!";
        newPassword = "newPassword1!";
        role = Role.USER;


        String key = "127.0.0.1:" + username;
        loginFailureCacheService.evictLoginFailureInfo(key);
        loginBlockCacheService.evict(key);
    }


    @Test
    @DisplayName("로그인 성공")
    public void loginSuccess() throws Exception {

        savedMember = registerMember(username, password, email, role);

        String loginRequestJson = String.format("""
                    {
                        "username": "%s",
                        "password": "%s"
                    }
                """, username, password);

        Map<String, Matcher<?>> expectedData = Map.of(
                "memberId", notNullValue(),
                "username", is(username),
                "email", is(email),
                "profilePic", is(profilePic),
                "role", is(role.getValue()),
                "accessToken", notNullValue(),
                "passwordResetToken", notNullValue()
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(cookie().exists("Refresh"))
                .andExpect(status().isOk())
                .andExpect(apiResponse(HttpStatus.OK.value(), expectedData));

        verify(memberAccessLogService, times(1)).
                writeLoginSuccessAccessLog(eq(savedMember.getId()), anyString());
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 사용자")
    public void loginFailsWhenUserDoesNotExist() throws Exception {

        String loginRequestJson = """
                    {
                        "username": "wrong.account",
                        "password": "wrongPassword@"
                    }
                """;

        ErrorCode errorCode = ErrorCode.INVALID_USERNAME_OR_PASSWORD;
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(errorResponse(errorCode));

    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 틀림")
    public void loginFailsWhenPasswordIsWrong() throws Exception {

        savedMember = registerMember(username, password, email, role);

        String loginRequestJson = String.format("""
                    {
                        "username": "%s",
                        "password": "wrongPassword@"
                    }
                """, username);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(errorResponse(ErrorCode.INVALID_USERNAME_OR_PASSWORD));

        verify(memberAccessLogService, times(1)).
                writeWrongPasswordAccessLog(contains(String.valueOf(savedMember.getUsername())), anyString());
    }

    @Test
    @DisplayName("로그인 실패 - 양식에 맞지 않는 요청")
    public void loginFailsWhenRequestIsInvalid() throws Exception {

        String loginRequestJson = """
                    {
                        "username": "test.123"
                    }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }


    @Test
    @DisplayName("로그인을 5분 이내에 5번 로그인 실패한 회원은 로그인 불가")
    public void loginFailsIfAttemptsExceededWithin5Minutes() throws Exception {

        savedMember = registerMember(username, password, email, role);

        String loginRequestJson = String.format("""
                    {
                        "username": "%s",
                        "password": "wrongPassword@"
                    }
                """, username);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequestJson));
        }

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(errorResponse(ErrorCode.BLOCKED_MEMBER));
    }

    @Test
    @DisplayName("회원 로그아웃 성공")
    public void logoutSuccess() throws Exception {

        MvcResult mvcResult =
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(USER_LOGIN_REQUEST))
                        .andReturn();

        String accessToken = getAccessToken(mvcResult);

        mockMvc.perform(post("/auth/logout")
                        .with(setAccessToken(accessToken)))
                .andExpect(status().isOk());

        tokenBlackListCacheService.evict(accessToken);
    }

    @Test
    @DisplayName("로그인 하지 않은 사용자는 로그아웃 불가")
    public void logoutFailsWhenUserIsNotLoggedIn() throws Exception {

        mockMvc.perform(post("/auth/logout"))
                .andExpect(errorResponse(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    public void tokenReissueSuccess() throws Exception {

        Map<String, Matcher<?>> expectedData = Map.of(
                "memberId", notNullValue(),
                "username", notNullValue(),
                "email", notNullValue(),
                "profilePic", notNullValue(),
                "role", is("USER"),
                "accessToken", notNullValue(),
                "passwordResetToken", notNullValue()
        );

        mockMvc.perform(post("/auth/refresh")
                        .with(authenticatedAsUser(mockMvc)))
                .andExpect(apiResponse(HttpStatus.OK.value(), expectedData));
    }

    @Test
    @DisplayName("토큰 재발급 실패 - refreshToken 누락")
    public void tokenReissueFailsWhenRefreshTokenDoesNotExist() throws Exception {

        MvcResult mvcResult =
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(USER_LOGIN_REQUEST))
                        .andReturn();

        String accessToken = getAccessToken(mvcResult);

        mockMvc.perform(post("/auth/refresh")
                        .with(setAccessToken(accessToken)))
                .andExpect(errorResponse(ErrorCode.INVALID_REFRESH_TOKEN));

    }

    @Test
    @DisplayName("토큰 재발급 실패 - 동일한 refreshToken으로 두 번 이상 재발급 불가")
    public void tokenReissueFailsWhenRefreshTokenIsReused() throws Exception {

        MvcResult mvcResult =
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(USER_LOGIN_REQUEST))
                        .andReturn();

        String accessToken = getAccessToken(mvcResult);
        String refreshToken = getRefreshToken(mvcResult);

        mockMvc.perform(post("/auth/refresh")
                        .with(setAccessToken(accessToken))
                        .with(setRefreshToken(refreshToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/refresh")
                        .with(setAccessToken(accessToken))
                        .with(setRefreshToken(refreshToken)))
                .andExpect(errorResponse(ErrorCode.INVALID_REFRESH_TOKEN));
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 인증이 필요한 리소스에 접근 불가")
    public void denyUnauthenticatedAccess() throws Exception {

        mockMvc.perform(get("/members/stats/role"))
                .andExpect(errorResponse(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    @DisplayName("인가되지 않은 사용자는 인가를 요구하는 리소스에 접근 불가")
    public void denyUnAuthorizedAccess() throws Exception {

        mockMvc.perform(get("/members/stats/role")
                        .with(authenticatedAsUser(mockMvc)))
                .andExpect(errorResponse(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("회원 단건 조회 성공")
    public void readMemberInfoSuccess() throws Exception {

        Map<String, Matcher<?>> expectedData = Map.of(
                "memberId", notNullValue(),
                "username", notNullValue(),
                "email", notNullValue(),
                "profilePic", notNullValue(),
                "role", notNullValue()
        );

        mockMvc.perform(get("/members/{memberId}", 1L)
                        .with(authenticatedAsUser(mockMvc)))
                .andExpect(apiResponse(HttpStatus.OK.value(), expectedData));

    }

    @Test
    @DisplayName("회원 단건 조회 실패 - 존재하지 않는 사용자")
    public void readMemberInfoFailsWhenUserDoesNotExist() throws Exception {

        mockMvc.perform(get("/members/{memberId}", 100000L)
                        .with(authenticatedAsUser(mockMvc)))
                .andExpect(errorResponse(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    @DisplayName("회원 페이지네이션 조회 성공")
    public void readMemberInfoListSuccess() throws Exception {

        Map<String, Matcher<?>> expectedData = Map.of(
                "page", is(1),
                "size", is(10),
                "totalPages", greaterThan(0),
                "totalMembers", greaterThan(0),
                "members[0].memberId", notNullValue(),
                "members[0].username", notNullValue(),
                "members[0].email", notNullValue(),
                "members[0].profilePic", notNullValue(),
                "members[0].role", is("USER")
        );

        mockMvc.perform(get("/members")
                        .param("role", "USER")
                        .param("page", "1")
                        .param("size", "10")
                        .with(authenticatedAsManager(mockMvc)))
                .andExpect(apiResponse(HttpStatus.OK.value(), expectedData));
    }

    @Test
    @DisplayName("회원 페이지네이션 조회 실패 - 양식에 맞지 않는 요청")
    public void readMemberInfoListFailsWhenRequestIsInvalid() throws Exception {

        mockMvc.perform(get("/members")
                        .param("role", "GUEST")
                        .param("page", "1")
                        .param("size", "10")
                        .with(authenticatedAsManager(mockMvc)))
                .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("소프트 딜리트 된 회원 조회 성공")
    public void readSoftDeletedMemberSuccess() throws Exception {

        savedMember = registerMember(username, password, email, role);
        savedMember.softDelete();

        Map<String, Matcher<?>> expectedData = Map.of(
                "page", is(1),
                "size", is(10),
                "totalPages", greaterThan(0),
                "totalMembers", greaterThan(0),
                "members[0].memberId", notNullValue(),
                "members[0].username", notNullValue(),
                "members[0].email", notNullValue(),
                "members[0].profilePic", notNullValue(),
                "members[0].role", notNullValue()
        );

        mockMvc.perform(get("/members/trash")
                        .param("page", "1")
                        .param("size", "10")
                        .with(authenticatedAsAdmin(mockMvc)))
                .andExpect(apiResponse(HttpStatus.OK.value(), expectedData));
    }

    @Test
    @DisplayName("소프트 딜리트 된 회원 조회 실패 - 양식에 맞지 않는 요청")
    public void readSoftDeletedMemberFailsWhenRequestIsInvalid() throws Exception {

        mockMvc.perform(get("/members/trash")
                        .param("page", "0")
                        .param("size", "10")
                        .with(authenticatedAsAdmin(mockMvc)))
                .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("회원 등록 성공")
    public void registerMemberSuccess() throws Exception {

        MemberRegistrationRequest request = new MemberRegistrationRequest(username, email, role.getValue());

        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authenticatedAsAdmin(mockMvc)))
                .andExpect(apiResponse(HttpStatus.CREATED.value(), null));

        verify(eventPublisher, times(1))
                .publishEvent(ArgumentMatchers.any(MemberRegisteredEvent.class));
    }

    @Test
    @DisplayName("회원 등록 실패 - 양식에 맞지 않는 요청")
    public void registerMemberFailsWhenRequestIsInvalid() throws Exception {

        username = "new123";
        email = "invalidEmail";

        MemberRegistrationRequest request = new MemberRegistrationRequest(username, email, role.getValue());

        registerMember(username, password, email, role);

        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authenticatedAsAdmin(mockMvc)))
                .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("회원 등록 실패 - 사용자명 중복")
    public void registerMemberFailsWhenUsernameIsDuplicated() throws Exception {

        MemberRegistrationRequest request = new MemberRegistrationRequest(username, email, role.getValue());

        registerMember(username, "password1!", "newUser1@email.com", role);

        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authenticatedAsAdmin(mockMvc)))
                .andExpect(errorResponse(ErrorCode.DUPLICATE_USERNAME));
    }

    @Test
    @DisplayName("회원 등록 실패 - 이메일 중복")
    public void registerMemberFailsWhenEmailIsDuplicated() throws Exception {

        MemberRegistrationRequest request = new MemberRegistrationRequest(username, email, role.getValue());

        registerMember("new.user", "password1!", email, role);

        mockMvc.perform(post("/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authenticatedAsAdmin(mockMvc)))
                .andExpect(errorResponse(ErrorCode.DUPLICATE_EMAIL));
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    public void changePasswordSuccess() throws Exception {

        savedMember = registerMember(username, originalPassword, email, role);

        String loginRequestJson = String.format("""
                    {
                        "username": "%s",
                        "password": "%s"
                    }
                """, username, originalPassword);

        MvcResult mvcResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = getAccessToken(mvcResult);
        PasswordChangeRequest request = new PasswordChangeRequest(originalPassword, newPassword);

        mockMvc.perform(put("/members/{memberId}/password", savedMember.getId())
                        .with(setAccessToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(apiResponse(HttpStatus.OK.value(), null));
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 다른 사람의 비밀번호를 변경")
    public void changePasswordFailsWhenTryingToChangeAnotherMembersPassword() throws Exception {

        savedMember = registerMember(username, originalPassword, email, role);

        String loginRequestJson = String.format("""
                    {
                        "username": "%s",
                        "password": "%s"
                    }
                """, username, originalPassword);

        MvcResult mvcResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = getAccessToken(mvcResult);
        PasswordChangeRequest request = new PasswordChangeRequest(originalPassword, newPassword);

        mockMvc.perform(put("/members/{memberId}/password", 1L)
                        .with(setAccessToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(errorResponse(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 기존 비밀번호가 틀림")
    public void changePasswordFailsWhenOriginalPasswordIsWrong() throws Exception {

        savedMember = registerMember(username, originalPassword, email, role);

        String loginRequestJson = String.format("""
                    {
                        "username": "%s",
                        "password": "%s"
                    }
                """, username, originalPassword);

        MvcResult mvcResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = getAccessToken(mvcResult);
        PasswordChangeRequest request = new PasswordChangeRequest("originalPassword2@", newPassword);

        mockMvc.perform(put("/members/{memberId}/password", savedMember.getId())
                        .with(setAccessToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(errorResponse(ErrorCode.INVALID_ORIGINAL_PASSWORD));
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 기존 비밀번호와 새 비밀번호가 같음")
    public void changePasswordFailsWhenNewPasswordIsSameAsOriginal() throws Exception {

        savedMember = registerMember(username, originalPassword, email, role);

        String loginRequestJson = String.format("""
                    {
                        "username": "%s",
                        "password": "%s"
                    }
                """, username, originalPassword);

        MvcResult mvcResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = getAccessToken(mvcResult);
        PasswordChangeRequest request = new PasswordChangeRequest(originalPassword, originalPassword);

        mockMvc.perform(put("/members/{memberId}/password", savedMember.getId())
                        .with(setAccessToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(errorResponse(ErrorCode.INVALID_NEW_PASSWORD));
    }

    @Test
    @DisplayName("비밀번호 초기화 성공")
    public void passwordResetSuccess() throws Exception {

        savedMember = registerMember(username, originalPassword, email, role);

        String passwordResetToken = jwtService.createPasswordResetToken(savedMember.getId());
        PasswordResetRequest request = new PasswordResetRequest(passwordResetToken, newPassword);

        mockMvc.perform(put("/members/{memberId}/password-reset", savedMember.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(apiResponse(HttpStatus.OK.value(), null));
    }

    @Test
    @DisplayName("비밀번호 초기화 실패 - 다른 사용자의 비밀번호를 초기화")
    public void passwordResetFailsWhenToChangeAnotherMembersPassword() throws Exception {

        savedMember = registerMember(username, originalPassword, email, role);

        String passwordResetToken = jwtService.createPasswordResetToken(savedMember.getId());
        PasswordResetRequest request = new PasswordResetRequest(passwordResetToken, newPassword);

        mockMvc.perform(put("/members/{memberId}/password-reset", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(errorResponse(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("비밀번호 초기화 실패 - 유효하지 않은 비밀번호 초기화 토큰")
    public void passwordResetFailsWhenPasswordResetTokenIsInvalid() throws Exception {

        savedMember = registerMember(username, originalPassword, email, role);

        String passwordResetToken = "invalidPasswordResetToken";
        PasswordResetRequest request = new PasswordResetRequest(passwordResetToken, newPassword);

        mockMvc.perform(put("/members/{memberId}/password-reset", savedMember.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(errorResponse(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));
    }

    @Test
    @DisplayName("비밀번호 초기화 이메일 전송 성공")
    public void sendPasswordResetEmailSuccess() throws Exception {

        savedMember = registerMember(username, originalPassword, email, role);
        PasswordResetEmailRequest request = new PasswordResetEmailRequest(savedMember.getUsername());

        mockMvc.perform(post("/members/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(apiResponse(HttpStatus.OK.value(), null));

        verify(eventPublisher, times(1)).
                publishEvent(ArgumentMatchers.any(PasswordResetMailEvent.class));
    }

    @Test
    @DisplayName("프로필 사진 업데이트 성공")
    public void updateProfilePicSuccess() throws Exception {

        savedMember = registerMember(username, password, email, role);

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());

        when(s3Service.isImageType(anyString())).thenReturn(true);
        when(s3Service.uploadFile(eq(file), anyString()))
                .thenReturn("newProfilePic");

        Map<String, Matcher<?>> expectedData = Map.of(
                "profilePic", notNullValue()
        );

        String accessToken = jwtService.createAccessToken(
                savedMember.getId(),
                username,
                savedMember.getProfilePic(),
                savedMember.getRole()
        );

        mockMvc.perform(multipart("/members/{memberId}/profile-pic", savedMember.getId())
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            request.addHeader("Authorization", "Bearer " + accessToken);
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(apiResponse(HttpStatus.OK.value(), expectedData));
    }

    @Test
    @DisplayName("프로필 사진 업데이트 실패 - 유효하지 않은 이미지 파일")
    public void updateProfilePicFailsWhenFileIsNotImageType() throws Exception {

        savedMember = registerMember(username, password, email, role);

        String accessToken = jwtService.createAccessToken(
                savedMember.getId(),
                username,
                savedMember.getProfilePic(),
                savedMember.getRole()
        );

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());

        when(s3Service.isImageType(anyString())).thenReturn(false);

        mockMvc.perform(multipart("/members/{memberId}/profile-pic", savedMember.getId())
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            request.addHeader("Authorization", "Bearer " + accessToken);
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("프로필 사진 업데이트 실패 - 이미지 파일이 5MB를 넘어가는 경우")
    public void updateProfilePicFailsWhenFileIsExceedsSizeLimit() throws Exception {

        savedMember = registerMember(username, password, email, role);

        String accessToken = jwtService.createAccessToken(
                savedMember.getId(),
                username,
                savedMember.getProfilePic(),
                savedMember.getRole()
        );

        byte[] fileContent = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", fileContent);

        when(s3Service.isImageType(anyString())).thenReturn(true);

        mockMvc.perform(multipart("/members/{memberId}/profile-pic", savedMember.getId())
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            request.addHeader("Authorization", "Bearer " + accessToken);
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(errorResponse(ErrorCode.TOO_LARGE_FILE));
    }

    @Test
    @DisplayName("프로필 사진 업데이트 실패 - Object Storage 클라이언트 문제 발생")
    public void updateProfilePicFailsWhenObjectStorageClientErrorOccurs() throws Exception {

        savedMember = registerMember(username, password, email, role);

        String accessToken = jwtService.createAccessToken(
                savedMember.getId(),
                username,
                savedMember.getProfilePic(),
                savedMember.getRole()
        );

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());

        when(s3Service.isImageType(anyString())).thenReturn(true);
        when(s3Service.uploadFile(eq(file), anyString()))
                .thenThrow(new ApiException(ErrorCode.OBJECT_STORAGE_ERROR));

        mockMvc.perform(multipart("/members/{memberId}/profile-pic", savedMember.getId())
                        .file(file)
                        .with(request -> {
                            request.setMethod("PUT");
                            request.addHeader("Authorization", "Bearer " + accessToken);
                            return request;
                        })
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(errorResponse(ErrorCode.OBJECT_STORAGE_ERROR));
    }

    @Test
    @DisplayName("권한 변경 성공")
    public void updateRoleSuccess() throws Exception {

        savedMember = registerMember(username, password, email, role);

        RoleUpdateRequest request = new RoleUpdateRequest(Role.MANAGER.getValue());

        mockMvc.perform(put("/members/{memberId}/role", savedMember.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authenticatedAsAdmin(mockMvc)))
                .andExpect(apiResponse(HttpStatus.OK.value(), null));
    }

    @Test
    @DisplayName("권한 변경 실패 - 이전과 동일한 권한")
    public void updateRoleFailsWhenRoleIsUnchanged() throws Exception {

        savedMember = registerMember(username, password, email, Role.USER);

        RoleUpdateRequest request = new RoleUpdateRequest(Role.USER.getValue());

        mockMvc.perform(put("/members/{memberId}/role", savedMember.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authenticatedAsAdmin(mockMvc)))
                .andExpect(errorResponse(ErrorCode.INVALID_NEW_ROLE));
    }

    @Test
    @DisplayName("소프트 딜리트 성공")
    public void softDeleteSuccess() throws Exception {

        savedMember = registerMember(username, password, email, role);

        mockMvc.perform(delete("/members/{memberId}", savedMember.getId())
                        .with(authenticatedAsAdmin(mockMvc)))
                .andExpect(apiResponse(HttpStatus.OK.value(), null));

        Assertions.assertThat(savedMember.getDeletedAt()).isNotNull();
        verify(eventPublisher, times(1))
                .publishEvent(ArgumentMatchers.any(SoftDeletedEvent.class));
    }

    @Test
    @DisplayName("소프트 딜리트 실패 - 이미 소프트 딜리트된 사용자")
    public void softDeleteFailsWhenUserAlreadyIsSoftDeleted() throws Exception {

        savedMember = registerMember(username, password, email, role);
        savedMember.softDelete();

        mockMvc.perform(delete("/members/{memberId}", savedMember.getId())
                        .with(authenticatedAsAdmin(mockMvc)))
                .andExpect(errorResponse(ErrorCode.MEMBER_ALREADY_SOFT_DELETED));
    }

    @Test
    @DisplayName("회원 복구 성공")
    public void restoreSuccess() throws Exception {

        savedMember = registerMember(username, password, email, role);
        savedMember.softDelete();

        mockMvc.perform(patch("/members/trash/{memberId}/restore", savedMember.getId())
                        .with(authenticatedAsAdmin(mockMvc)))
                .andExpect(apiResponse(HttpStatus.OK.value(), null));
    }

    @Test
    @DisplayName("회원 복구 실패 - 소프트 딜리트된 사용자가 아님")
    public void restoreFailsWhenUserIsNotSoftDeleted() throws Exception {

        savedMember = registerMember(username, password, email, role);

        mockMvc.perform(patch("/members/trash/{memberId}/restore", savedMember.getId())
                        .with(authenticatedAsAdmin(mockMvc)))
                .andExpect(errorResponse(ErrorCode.MEMBER_NOT_SOFT_DELETED));
    }

    private Member registerMember(String username, String password, String email, Role role) {

        Member member = Member.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(role)
                .email(email)
                .profilePic(profilePic)
                .build();

        Member savedMember = memberRepository.save(member);
        memberRepository.flush();

        return savedMember;
    }

    private String getAccessToken(MvcResult mvcResult) throws Exception {
        String responseContent = mvcResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseContent);

        return jsonNode.path("data").path("accessToken").asText();
    }

    private String getRefreshToken(MvcResult mvcResult) {
        return mvcResult.getResponse().getCookie("Refresh").getValue();
    }

}
