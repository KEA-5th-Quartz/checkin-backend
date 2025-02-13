package com.quartz.checkin.integration;

import static com.quartz.checkin.util.ApiResponseMatchers.*;
import static com.quartz.checkin.util.ErrorResponseMatchers.*;
import static com.quartz.checkin.util.RequestAuthPostProcessor.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Role;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.security.service.JwtService;
import com.quartz.checkin.service.LoginBlockCacheService;
import com.quartz.checkin.service.MemberAccessLogService;
import java.util.Map;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles(value = "test")
@AutoConfigureMockMvc
@SpringBootTest
@Sql(scripts = "classpath:data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Transactional
public class MemberCommonIntegrationTest {

    @Autowired
    JwtService jwtService;

    @Autowired
    LoginBlockCacheService loginBlockCacheService;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MemberAccessLogService memberAccessLogService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Value("${user.profile.defaultImageUrl}")
    private String profilePic;

    @Test
    @DisplayName("로그인 성공")
    public void loginSuccess() throws Exception {
        Member member = Member.builder()
                .username("test.account")
                .password(passwordEncoder.encode("testPassword1@"))
                .role(Role.USER)
                .email("testAccount@email.com")
                .profilePic(profilePic)
                .build();

        Member savedMember = memberRepository.save(member);
        memberRepository.flush();

        String loginRequestJson = """
                    {
                        "username": "test.account",
                        "password": "testPassword1@"
                    }
                """;

        Map<String, Matcher<?>> expectedData = Map.of(
                "memberId", notNullValue(),
                "username", is("test.account"),
                "email", is("testAccount@email.com"),
                "profilePic", is(profilePic),
                "role", is("USER"),
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

        Member member = Member.builder()
                .username("test.account")
                .password(passwordEncoder.encode("testPassword1@"))
                .role(Role.USER)
                .email("testAccount@email.com")
                .profilePic(profilePic)
                .build();

        Member savedMember = memberRepository.save(member);
        memberRepository.flush();

        String loginRequestJson = """
                    {
                        "username": "test.account",
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
    @DisplayName("로그인 실패 - 양식에 맞지 않는 요청")
    public void loginFailsWhenRequestIsInvalid() throws Exception {

        String loginRequestJson = """
                    {
                        "username": "test.123"
                    }
                """;

        ErrorCode errorCode = ErrorCode.INVALID_DATA;
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(errorResponse(errorCode));
    }


    @Test
    @DisplayName("로그인을 5분 이내에 5번 로그인 실패한 회원은 로그인 불가")
    public void loginFailsIfAttemptsExceededWithin5Minutes() throws Exception {

        Member member = Member.builder()
                .username("new.account")
                .password(passwordEncoder.encode("testPassword1@"))
                .role(Role.USER)
                .email("newAccount@email.com")
                .profilePic(profilePic)
                .build();

        Member savedMember = memberRepository.save(member);
        memberRepository.flush();

        String loginRequestJson = """
                    {
                        "username": "new.account",
                        "password": "wrongPassword@"
                    }
                """;

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginRequestJson));
        }

        ErrorCode errorCode = ErrorCode.BLOCKED_MEMBER;
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequestJson))
                .andExpect(errorResponse(errorCode));
    }

    @Test
    @DisplayName("회원 로그아웃 성공")
    public void logoutSuccess() throws Exception {

        mockMvc.perform(post("/auth/logout")
                        .with(authenticatedAsUser(mockMvc)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그인 하지 않은 사용자는 로그아웃 불가")
    public void logoutFailsWhenUserIsNotLoggedIn() throws Exception {

        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
        mockMvc.perform(post("/auth/logout"))
                .andExpect(errorResponse(errorCode));
    }
}
