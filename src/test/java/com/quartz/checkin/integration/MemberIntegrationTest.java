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
import org.junit.jupiter.api.Nested;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles(value = "test")
@AutoConfigureMockMvc
@SpringBootTest
@Sql(scripts = "classpath:data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Transactional
public class MemberIntegrationTest {

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
    ObjectMapper objectMapper;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Value("${user.profile.defaultImageUrl}")
    private String profilePic;

    @Nested
    @DisplayName("로그인 테스트")
    class loginTests {

        @Test
        @DisplayName("로그인 성공")
        public void loginSuccess() throws Exception {

            String username = "test.account";
            String password = "testPassword1@";
            String email = "testAccount@email.com";
            Role role = Role.USER;

            Member savedMember = registerMember(username, password, email, role);

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

            String username = "test.account";
            String password = "testPassword1@";
            String email = "testAccount@email.com";
            Role role = Role.USER;

            Member savedMember = registerMember(username, password, email, role);

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

            String username = "new.account";
            String password = "testPassword1@";
            String email = "newAccount@email.com";
            Role role = Role.USER;

            Member savedMember = registerMember(username, password, email, role);

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

    }


    @Nested
    @DisplayName("로그아웃 테스트")
    class logoutTests {
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

            mockMvc.perform(post("/auth/logout"))
                    .andExpect(errorResponse(ErrorCode.UNAUTHENTICATED));
        }
    }

    @Nested
    @DisplayName("토큰 재발급 테스트")
    class tokenReissueTests {

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

    }

    @Nested
    @DisplayName("인증 테스트")
    class AuthenticationTests {

        @Test
        @DisplayName("인증되지 않은 사용자는 인증이 필요한 리소스에 접근 불가")
        public void denyUnauthenticatedAccess() throws Exception {

            mockMvc.perform(get("/members/stats/role"))
                    .andExpect(errorResponse(ErrorCode.UNAUTHENTICATED));
        }

    }

    @Nested
    @DisplayName("인가 테스트")
    class AuthorizationTests {

        @Test
        @DisplayName("인가되지 않은 사용자는 인가를 요구하는 리소스에 접근 불가")
        public void denyUnAuthorizedAccess() throws Exception {

            mockMvc.perform(get("/members/stats/role")
                            .with(authenticatedAsUser(mockMvc)))
                    .andExpect(errorResponse(ErrorCode.FORBIDDEN));
        }

    }

    @Nested
    @DisplayName("회원 단건 조회 테스트")
    class ReadMemberInfoTests {

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

    }

    @Nested
    @DisplayName("회원 페이지네이션 조회 테스트")
    class ReadMemberInfoListTests {
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

    }

    @Nested
    @DisplayName("소프트 딜리트 된 회원 조회 테스트")
    class SoftDeletedMemberQueryTests {

        @Test
        @DisplayName("소프트 딜리트 된 회원 조회 성공")
        public void readSoftDeletedMemberSuccess() throws Exception {

            String username = "new.account";
            String password = "testPassword1@";
            String email = "newAccount@email.com";
            Role role = Role.USER;

            Member savedMember = registerMember(username, password, email, role);
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
