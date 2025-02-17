package com.quartz.checkin.unit.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.template.request.TemplateSaveRequest;
import com.quartz.checkin.entity.Attachment;
import com.quartz.checkin.entity.Category;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Role;
import com.quartz.checkin.entity.Template;
import com.quartz.checkin.entity.TemplateAttachment;
import com.quartz.checkin.repository.AttachmentRepository;
import com.quartz.checkin.repository.TemplateAttachmentRepository;
import com.quartz.checkin.repository.TemplateRepository;
import com.quartz.checkin.security.CustomUser;
import com.quartz.checkin.service.CategoryServiceImpl;
import com.quartz.checkin.service.MemberService;
import com.quartz.checkin.service.TemplateService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
public class TemplateServiceTest {

    @Mock
    AttachmentRepository attachmentRepository;
    @Mock
    CategoryServiceImpl categoryService;
    @Mock
    MemberService memberService;
    @Mock
    TemplateAttachmentRepository templateAttachmentRepository;
    @Mock
    TemplateRepository templateRepository;
    @InjectMocks
    TemplateService templateService;


    private Long id;
    private Category firstCategory;
    private Category secondCategory;
    private CustomUser customUser;
    private Member existingMember;
    private Template template;
    private List<Attachment> attachments;

    @BeforeEach
    public void setUp() {
        id = 1L;

        existingMember = Member.builder()
                .id(id)
                .build();

        customUser = new CustomUser(
                1L,
                "user.a",
                "password1!",
                "userA@email.com",
                "profilePic",
                Role.USER,
                null,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + Role.USER.getValue()))
        );

        firstCategory = new Category(null, "firstCategrt", "fc", null);
        secondCategory = new Category(firstCategory, "firstCategrt", "fc", null);

        attachments = List.of(new Attachment("attachment1"), new Attachment("attachment2"));

        template = Template.builder()
                .id(id)
                .member(existingMember)
                .firstCategory(firstCategory)
                .secondCategory(secondCategory)
                .title("title")
                .content("content")
                .build();
    }

    @Nested
    @DisplayName("템플릿 생성 테스트")
    class TemplateCreateTests {

        private TemplateSaveRequest request;

        @BeforeEach
        public void setUp() {

            request = new TemplateSaveRequest(
                    "title",
                    firstCategory.getName(),
                    secondCategory.getName(),
                    "content",
                    List.of(1L, 2L));
        }

        @Test
        @DisplayName("템플릿 생성 성공")
        public void templateCreateSuccess() {
            //given
            when(memberService.getMemberByIdOrThrow(id)).thenReturn(existingMember);
            when(categoryService.getFirstCategoryOrThrow(firstCategory.getName())).thenReturn(firstCategory);
            when(categoryService.getSecondCategoryOrThrow(secondCategory.getName(), firstCategory))
                    .thenReturn(secondCategory);

            when(attachmentRepository.findAllById(request.getAttachmentIds())).thenReturn(attachments);
            when(templateRepository.save(any(Template.class))).thenReturn(template);

            ArgumentCaptor<List<TemplateAttachment>> templateAttachmentCaptor = ArgumentCaptor.forClass(List.class);

            //when
            templateService.createTemplate(request, customUser);

            //then
            verify(templateRepository).save(any(Template.class));
            verify(templateAttachmentRepository).saveAll(templateAttachmentCaptor.capture());

            List<TemplateAttachment> capturedValue = templateAttachmentCaptor.getValue();
            assertThat(capturedValue).hasSize(attachments.size());
            assertThat(capturedValue)
                    .extracting(TemplateAttachment::getAttachment)
                    .containsExactlyInAnyOrderElementsOf(attachments);
        }

        @Test
        @DisplayName("템플릿 생성 실페 - 존재하지 않는 사용자")
        public void templateCreateFailsWhenUserDoesNotExist() {
            //given
            when(memberService.getMemberByIdOrThrow(id)).thenThrow(new ApiException(ErrorCode.MEMBER_NOT_FOUND));

            //when & then
            assertThatThrownBy(() -> templateService.createTemplate(request, customUser))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.MEMBER_NOT_FOUND));
        }

        @Test
        @DisplayName("템플릿 생성 실페 - 존재하지 않는 1차 카테고리")
        public void templateCreateFailsWhenFirstCategoryDoesNotExist() {
            //given
            when(memberService.getMemberByIdOrThrow(id)).thenReturn(existingMember);
            when(categoryService.getFirstCategoryOrThrow(firstCategory.getName()))
                    .thenThrow(new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST));

            //when & then
            assertThatThrownBy(() -> templateService.createTemplate(request, customUser))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.CATEGORY_NOT_FOUND_FIRST));
        }

        @Test
        @DisplayName("템플릿 생성 실페 - 존재하지 않는 2차 카테고리")
        public void templateCreateFailsWhenSecondCategoryDoesNotExist() {
            //given
            when(memberService.getMemberByIdOrThrow(id)).thenReturn(existingMember);
            when(categoryService.getFirstCategoryOrThrow(firstCategory.getName())).thenReturn(firstCategory);
            when(categoryService.getSecondCategoryOrThrow(secondCategory.getName(), firstCategory))
                    .thenThrow(new ApiException(ErrorCode.CATEGORY_NOT_FOUND_SECOND));

            //when & then
            assertThatThrownBy(() -> templateService.createTemplate(request, customUser))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.CATEGORY_NOT_FOUND_SECOND));
        }
    }


}
