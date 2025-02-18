package com.quartz.checkin.integration;

import static com.quartz.checkin.util.ApiResponseMatchers.*;
import static com.quartz.checkin.util.ErrorResponseMatchers.*;
import static com.quartz.checkin.util.RequestAuthPostProcessor.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.template.request.TemplateDeleteRequest;
import com.quartz.checkin.dto.template.request.TemplateSaveRequest;
import com.quartz.checkin.dto.template.response.TemplateSimpleResponse;
import com.quartz.checkin.entity.Category;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Role;
import com.quartz.checkin.entity.Template;
import com.quartz.checkin.repository.CategoryRepository;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.repository.TemplateRepository;
import com.quartz.checkin.security.service.JwtService;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles(value = "test")
@AutoConfigureMockMvc
@SpringBootTest
@Sql(scripts = "classpath:data.sql", executionPhase = ExecutionPhase.BEFORE_TEST_CLASS)
@Transactional
public class TemplateIntegrationTest {

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    JwtService jwtService;

    @Autowired
    TemplateRepository templateRepository;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String content;
    private String title;
    private String firstCategoryName;
    private String secondCategoryName;
    private Category firstCategory;
    private Category secondCategory;
    private Member member;


    @BeforeEach
    public void setUp() {
        content = "content";
        title = "title";
        firstCategoryName = "firstCategory";
        secondCategoryName = "secondCategory";

        member = Member.builder()
                .username("test.a")
                .email("testA@email.com")
                .password("password1!")
                .profilePic("profilePic")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("템플릿 생성 성공")
    public void createTemplateSuccess() throws Exception {

        initCategory();

        TemplateSaveRequest request = new TemplateSaveRequest(
                title,
                firstCategoryName,
                secondCategoryName,
                content,
                List.of()
        );

        Map<String, Matcher<?>> expectedData = Map.of(
                "templateId", notNullValue()
        );

        mockMvc.perform(post("/members/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authenticatedAsUser(mockMvc)))
                .andExpect(apiResponse(HttpStatus.OK.value(), expectedData));

    }

    @Test
    @DisplayName("템플릿 생성 실패 - 존재하지 않는 1차 카테고리")
    public void createTemplateFailsWhenFirstCategoryDoesNotExist() throws Exception {

        initCategory();

        TemplateSaveRequest request = new TemplateSaveRequest(
                title,
                "invalidFirstCategoryName",
                secondCategoryName,
                content,
                List.of()
        );

        mockMvc.perform(post("/members/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authenticatedAsUser(mockMvc)))
                .andExpect(errorResponse(ErrorCode.CATEGORY_NOT_FOUND_FIRST));
    }

    @Test
    @DisplayName("템플릿 생성 실패 - 존재하지 않는 2차 카테고리")
    public void createTemplateFailsWhenSecondCategoryDoesNotExist() throws Exception {

        initCategory();

        TemplateSaveRequest request = new TemplateSaveRequest(
                title,
                firstCategoryName,
                "invalidSecondCategoryName",
                content,
                List.of()
        );

        mockMvc.perform(post("/members/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authenticatedAsUser(mockMvc)))
                .andExpect(errorResponse(ErrorCode.CATEGORY_NOT_FOUND_SECOND));
    }

    @Test
    @DisplayName("템플릿 생성 실패 - 유효하지 않은 첨부파일")
    public void createTemplateFailsWhenAttachmentsAreInvalid() throws Exception {

        initCategory();

        TemplateSaveRequest request = new TemplateSaveRequest(
                title,
                firstCategoryName,
                secondCategoryName,
                content,
                List.of(1000L, 2000L)
        );

        mockMvc.perform(post("/members/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(authenticatedAsUser(mockMvc)))
                .andExpect(errorResponse(ErrorCode.ATTACHMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("템플릿 다중 삭제 성공")
    public void deleteTemplatesSuccess() throws Exception {

        initCategory();

        member = memberRepository.save(member);

        Template template1 = Template.builder()
                .member(member)
                .firstCategory(firstCategory)
                .secondCategory(secondCategory)
                .title("title1")
                .content("content1")
                .build();

        Template template2 = Template.builder()
                .member(member)
                .firstCategory(firstCategory)
                .secondCategory(secondCategory)
                .title("title2")
                .content("content2")
                .build();

        template1 = templateRepository.save(template1);
        template2 = templateRepository.save(template2);

        TemplateDeleteRequest request = new TemplateDeleteRequest(List.of(template1.getId(), template2.getId()));

        Map<String, Matcher<?>> expectedData = Map.of(
                "deletedTemplates", notNullValue()
        );

        String accessToken = jwtService.createAccessToken(
                member.getId(),
                member.getUsername(),
                member.getProfilePic(),
                member.getRole()
        );

        mockMvc.perform(delete("/members/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(setAccessToken(accessToken)))
                .andExpect(apiResponse(HttpStatus.OK.value(), expectedData));
    }

    @Test
    @DisplayName("템플릿 다중 삭제 실패 - 다른 사용자의 탬플릿 삭제")
    public void deleteTemplatesFailsWhenDeletingOthersTemplates() throws Exception {

        initCategory();

        member = memberRepository.save(member);
        Member anotherMember = memberRepository.findById(1L).get();

        Template template1 = Template.builder()
                .member(anotherMember)
                .firstCategory(firstCategory)
                .secondCategory(secondCategory)
                .title("title1")
                .content("content1")
                .build();

        Template template2 = Template.builder()
                .member(anotherMember)
                .firstCategory(firstCategory)
                .secondCategory(secondCategory)
                .title("title2")
                .content("content2")
                .build();

        template1 = templateRepository.save(template1);
        template2 = templateRepository.save(template2);

        TemplateDeleteRequest request = new TemplateDeleteRequest(List.of(template1.getId(), template2.getId()));

        String accessToken = jwtService.createAccessToken(
                member.getId(),
                member.getUsername(),
                member.getProfilePic(),
                member.getRole()
        );

        mockMvc.perform(delete("/members/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(setAccessToken(accessToken)))
                .andExpect(errorResponse(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("템플릿 업데이트 성공")
    public void updateTemplateSuccess() throws Exception {

        initCategory();

        member = memberRepository.save(member);

        Template template = Template.builder()
                .member(member)
                .firstCategory(firstCategory)
                .secondCategory(secondCategory)
                .title("title1")
                .content("content1")
                .build();

        template = templateRepository.save(template);

        TemplateSaveRequest request = new TemplateSaveRequest(
                title,
                firstCategoryName,
                secondCategoryName,
                content,
                List.of(1L, 2L)
        );

        String accessToken = jwtService.createAccessToken(
                member.getId(),
                member.getUsername(),
                member.getProfilePic(),
                member.getRole()
        );

        mockMvc.perform(put("/members/templates/{templateId}", template.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(setAccessToken(accessToken)))
                .andExpect(apiResponse(HttpStatus.OK.value(), null));
    }

    @Test
    @DisplayName("템플릿 업데이트 실패 - 존재하지 않는 템플릿")
    public void updateTemplateFailsWhenTemplateDoesNotExist() throws Exception {

        initCategory();

        member = memberRepository.save(member);

        TemplateSaveRequest request = new TemplateSaveRequest(
                title,
                firstCategoryName,
                secondCategoryName,
                content,
                List.of(1L, 2L)
        );

        String accessToken = jwtService.createAccessToken(
                member.getId(),
                member.getUsername(),
                member.getProfilePic(),
                member.getRole()
        );

        mockMvc.perform(put("/members/templates/{templateId}", 1000000L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(setAccessToken(accessToken)))
                .andExpect(errorResponse(ErrorCode.TEMPLATE_NOT_FOUND));
    }

    @Test
    @DisplayName("템플릿 업데이트 실패 - 존재하지 않는 사용자")
    public void updateTemplateFailsWhenUserDoesNotExist() throws Exception {

        initCategory();

        member = memberRepository.save(member);

        Template template = Template.builder()
                .member(member)
                .firstCategory(firstCategory)
                .secondCategory(secondCategory)
                .title("title1")
                .content("content1")
                .build();

        template = templateRepository.save(template);

        TemplateSaveRequest request = new TemplateSaveRequest(
                title,
                firstCategoryName,
                secondCategoryName,
                content,
                List.of(1L, 2L)
        );

        String accessToken = jwtService.createAccessToken(
                100000L,
                member.getUsername(),
                member.getProfilePic(),
                member.getRole()
        );

        mockMvc.perform(put("/members/templates/{templateId}", template.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(setAccessToken(accessToken)))
                .andExpect(errorResponse(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    @DisplayName("템플릿 업데이트 실패 - 다른 사용자의 템플릿을 업데이트")
    public void updateTemplateFailsWhenUpdatingOthersTemplates() throws Exception {

        initCategory();

        member = memberRepository.save(member);
        Member anotherMember = memberRepository.findById(1L).get();

        Template template = Template.builder()
                .member(anotherMember)
                .firstCategory(firstCategory)
                .secondCategory(secondCategory)
                .title("title1")
                .content("content1")
                .build();

        template = templateRepository.save(template);

        TemplateSaveRequest request = new TemplateSaveRequest(
                title,
                firstCategoryName,
                secondCategoryName,
                content,
                List.of(1L, 2L)
        );

        String accessToken = jwtService.createAccessToken(
                member.getId(),
                member.getUsername(),
                member.getProfilePic(),
                member.getRole()
        );

        mockMvc.perform(put("/members/templates/{templateId}", template.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(setAccessToken(accessToken)))
                .andExpect(errorResponse(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("템플릿 단건 조회 성공")
    public void readTemplateSuccess() throws Exception {

        initCategory();

        member = memberRepository.save(member);

        Template template = Template.builder()
                .member(member)
                .firstCategory(firstCategory)
                .secondCategory(secondCategory)
                .title("title1")
                .content("content1")
                .build();

        template = templateRepository.save(template);

        String accessToken = jwtService.createAccessToken(
                member.getId(),
                member.getUsername(),
                member.getProfilePic(),
                member.getRole()
        );

        Map<String, Matcher<?>> expectedData = Map.of(
                "templateId", notNullValue(),
                "title", notNullValue(),
                "firstCategory", notNullValue(),
                "secondCategory", notNullValue(),
                "content", notNullValue(),
                "attachmentIds", notNullValue()
        );

        mockMvc.perform(get("/members/templates/{templateId}", template.getId())
                        .with(setAccessToken(accessToken)))
                .andExpect(apiResponse(HttpStatus.OK.value(), expectedData));
    }

    @Test
    @DisplayName("템플릿 단건 조회 실패 - 존재하지 않는 템플릿")
    public void readTemplateFailsWhenTemplateDoesNotExist() throws Exception {

        initCategory();

        member = memberRepository.save(member);

        String accessToken = jwtService.createAccessToken(
                member.getId(),
                member.getUsername(),
                member.getProfilePic(),
                member.getRole()
        );

        mockMvc.perform(get("/members/templates/{templateId}", 100000L)
                        .with(setAccessToken(accessToken)))
                .andExpect(errorResponse(ErrorCode.TEMPLATE_NOT_FOUND));
    }

    @Test
    @DisplayName("템플릿 단건 조회 실패 - 다른 사용자의 템플릿을 조회")
    public void readTemplateFailsWhenReadingOthersTemplate() throws Exception {

        initCategory();

        member = memberRepository.save(member);
        Member anotherMember = memberRepository.findById(1L).get();

        Template template = Template.builder()
                .member(anotherMember)
                .firstCategory(firstCategory)
                .secondCategory(secondCategory)
                .title("title1")
                .content("content1")
                .build();

        template = templateRepository.save(template);

        String accessToken = jwtService.createAccessToken(
                member.getId(),
                member.getUsername(),
                member.getProfilePic(),
                member.getRole()
        );

        mockMvc.perform(get("/members/templates/{templateId}", template.getId())
                        .with(setAccessToken(accessToken)))
                .andExpect(errorResponse(ErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("템플릿 리스트 조회 성공")
    public void readTemplateListSuccess() throws Exception {

        initCategory();

        member = memberRepository.save(member);

        Template template1 = Template.builder()
                .member(member)
                .firstCategory(firstCategory)
                .secondCategory(secondCategory)
                .title("title1")
                .content("content1")
                .build();

        Template template2 = Template.builder()
                .member(member)
                .firstCategory(firstCategory)
                .secondCategory(secondCategory)
                .title("title2")
                .content("content2")
                .build();

        templateRepository.save(template1);
        templateRepository.save(template2);


        Map<String, Matcher<?>> expectedData = Map.of(
                "page", notNullValue(),
                "size", notNullValue(),
                "totalPages", notNullValue(),
                "totalTemplates", is(2),
                "templates", notNullValue()
        );

        String accessToken = jwtService.createAccessToken(
                member.getId(),
                member.getUsername(),
                member.getProfilePic(),
                member.getRole()
        );

        mockMvc.perform(get("/members/{memberId}/templates", member.getId())
                        .param("page", "1")
                        .param("size", "10")
                        .with(setAccessToken(accessToken)))
                .andExpect(apiResponse(HttpStatus.OK.value(), expectedData));
    }

    @Test
    @DisplayName("템플릿 리스트 조회 실패 - 양식에 맞지 않는 요청")
    public void readTemplateListFailsWhenRequestIsInvalid() throws Exception {

        initCategory();

        member = memberRepository.save(member);

        String accessToken = jwtService.createAccessToken(
                member.getId(),
                member.getUsername(),
                member.getProfilePic(),
                member.getRole()
        );

        mockMvc.perform(get("/members/{memberId}/templates", member.getId())
                        .param("page", "0")
                        .param("size", "10")
                        .with(setAccessToken(accessToken)))
                .andExpect(errorResponse(ErrorCode.INVALID_DATA));
    }

    @Test
    @DisplayName("템플릿 리스트 조회 실패 - 존재하지 않는 사용자")
    public void readTemplateListFailsWhenUserDoesNotExist() throws Exception {

        initCategory();

        member = memberRepository.save(member);

        String accessToken = jwtService.createAccessToken(
                member.getId(),
                member.getUsername(),
                member.getProfilePic(),
                member.getRole()
        );

        mockMvc.perform(get("/members/{memberId}/templates", 10000L)
                        .param("page", "1")
                        .param("size", "10")
                        .with(setAccessToken(accessToken)))
                .andExpect(errorResponse(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    @DisplayName("템플릿 리스트 조회 실패 - 다른 사용자의 템플릿 리스트 조회")
    public void readTemplateListFailsWhenReadingOthersTemplateList() throws Exception {

        initCategory();

        member = memberRepository.save(member);
        Member anotherMember = memberRepository.findById(1L).get();

        String accessToken = jwtService.createAccessToken(
                member.getId(),
                member.getUsername(),
                member.getProfilePic(),
                member.getRole()
        );

        mockMvc.perform(get("/members/{memberId}/templates", anotherMember.getId())
                        .param("page", "1")
                        .param("size", "10")
                        .with(setAccessToken(accessToken)))
                .andExpect(errorResponse(ErrorCode.FORBIDDEN));
    }

    private void initCategory() {
        firstCategory = categoryRepository.save(new Category(null, firstCategoryName, "fc", content));
        secondCategory = categoryRepository.save(new Category(firstCategory, secondCategoryName, "sc", content));
    }

}
