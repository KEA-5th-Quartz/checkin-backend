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
import com.quartz.checkin.dto.template.request.TemplateSaveRequest;
import com.quartz.checkin.entity.Category;
import com.quartz.checkin.repository.CategoryRepository;
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
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String content;
    private String title;
    private String firstCategoryName;
    private String secondCategoryName;

    @BeforeEach
    public void setUp() {
        content = "content";
        title = "title";
        firstCategoryName = "firstCategory";
        secondCategoryName = "secondCategory";
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

    private void initCategory() {

        Category firstCategory = new Category(null, firstCategoryName, "fc", content);
        Category secondCategory = new Category(firstCategory, secondCategoryName, "sc", content);
        categoryRepository.save(firstCategory);
        categoryRepository.save(secondCategory);

        categoryRepository.flush();
    }
}
