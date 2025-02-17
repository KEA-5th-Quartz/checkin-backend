package com.quartz.checkin.unit.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.quartz.checkin.common.AttachmentUtils;
import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.template.request.TemplateDeleteRequest;
import com.quartz.checkin.dto.template.request.TemplateSaveRequest;
import com.quartz.checkin.dto.template.response.TemplateDeleteResponse;
import com.quartz.checkin.dto.template.response.TemplateIdResponse;
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
import com.quartz.checkin.service.AttachmentService;
import com.quartz.checkin.service.CategoryServiceImpl;
import com.quartz.checkin.service.MemberService;
import com.quartz.checkin.service.TemplateService;
import jakarta.persistence.EntityManager;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
    AttachmentService attachmentService;
    @Mock
    AttachmentUtils attachmentUtils;
    @Mock
    CategoryServiceImpl categoryService;
    @Mock
    EntityManager entityManager;
    @Mock
    MemberService memberService;
    @Mock
    TemplateAttachmentRepository templateAttachmentRepository;
    @Mock
    TemplateRepository templateRepository;
    @InjectMocks
    TemplateService templateService;


    private Long memberId;
    private Long templateId;
    private Category firstCategory;
    private Category secondCategory;
    private CustomUser customUser;
    private Member existingMember;
    private Template template;
    private List<Attachment> attachments;

    @BeforeEach
    public void setUp() {
        memberId = 1L;
        templateId = 1L;

        existingMember = Member.builder()
                .id(memberId)
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

        firstCategory = new Category(null, "firstCategory", "fc", null);
        secondCategory = new Category(firstCategory, "secondCategory", "fc", null);

        attachments = List.of(new Attachment("attachment1"), new Attachment("attachment2"));

        template = Template.builder()
                .id(templateId)
                .member(existingMember)
                .firstCategory(firstCategory)
                .secondCategory(secondCategory)
                .title("title")
                .content("content")
                .build();
    }

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
            when(memberService.getMemberByIdOrThrow(memberId)).thenReturn(existingMember);
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
            when(memberService.getMemberByIdOrThrow(memberId)).thenThrow(new ApiException(ErrorCode.MEMBER_NOT_FOUND));

            //when & then
            assertThatThrownBy(() -> templateService.createTemplate(request, customUser))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.MEMBER_NOT_FOUND));
        }

        @Test
        @DisplayName("템플릿 생성 실페 - 존재하지 않는 1차 카테고리")
        public void templateCreateFailsWhenFirstCategoryDoesNotExist() {
            //given
            when(memberService.getMemberByIdOrThrow(memberId)).thenReturn(existingMember);
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
            when(memberService.getMemberByIdOrThrow(memberId)).thenReturn(existingMember);
            when(categoryService.getFirstCategoryOrThrow(firstCategory.getName())).thenReturn(firstCategory);
            when(categoryService.getSecondCategoryOrThrow(secondCategory.getName(), firstCategory))
                    .thenThrow(new ApiException(ErrorCode.CATEGORY_NOT_FOUND_SECOND));

            //when & then
            assertThatThrownBy(() -> templateService.createTemplate(request, customUser))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.CATEGORY_NOT_FOUND_SECOND));
        }

        @Test
        @DisplayName("템플릿 생성 실패 - 존재하지 않는 첨부파일")
        public void templateCreateFailsWhenAttachmentsAreInvalid() {
            //given
            when(memberService.getMemberByIdOrThrow(memberId)).thenReturn(existingMember);
            when(categoryService.getFirstCategoryOrThrow(firstCategory.getName())).thenReturn(firstCategory);
            when(categoryService.getSecondCategoryOrThrow(secondCategory.getName(), firstCategory))
                    .thenReturn(secondCategory);

            List<Attachment> searchedAttachments = List.of(new Attachment("attachment1"));
            when(attachmentRepository.findAllById(request.getAttachmentIds())).thenReturn(searchedAttachments);

            //when & then
            assertThatThrownBy(() -> templateService.createTemplate(request, customUser))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.ATTACHMENT_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("템플릿 업데이트 단위 테스트")
    class TemplateUpdateTests {

        private String newTitle;
        private String newContent;
        private Category newFirstCategory;
        private Category newSecondCategory;
        private TemplateSaveRequest request;

        @BeforeEach
        public void setUp() {

            newTitle = "newTitle";
            newContent = "newContent";

            newFirstCategory = new Category(null, "newFirstCategory", "nfc", null);
            newSecondCategory = new Category(newFirstCategory, "newSecondCategory", "nsc", null);

            request = new TemplateSaveRequest(
                    newTitle,
                    newFirstCategory.getName(),
                    newSecondCategory.getName(),
                    newContent,
                    List.of(2L, 3L));
        }

        @Test
        @DisplayName("템플릿 업데이트 성공")
        public void templateUpdateSuccess() {
            //given
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
            when(memberService.getMemberByIdOrThrow(memberId)).thenReturn(existingMember);

            when(categoryService.getFirstCategoryOrThrow(newFirstCategory.getName())).thenReturn(newFirstCategory);
            when(categoryService.getSecondCategoryOrThrow(newSecondCategory.getName(), newFirstCategory))
                    .thenReturn(newSecondCategory);

            //when
            templateService.updateTemplate(templateId, request, customUser);

            //then
            assertThat(template.getTitle()).isEqualTo(newTitle);
            assertThat(template.getContent()).isEqualTo(newContent);
            assertThat(template.getFirstCategory().getName()).isEqualTo(newFirstCategory.getName());
            assertThat(template.getSecondCategory().getName()).isEqualTo(newSecondCategory.getName());

            verify(attachmentUtils).handleTemplateAttachments(template, request.getAttachmentIds());
        }

        @Test
        @DisplayName("템플릿 업데이트 실패 - 존재하지 않는 템플릿")
        public void templateUpdateFailsWhenTemplateDoesNotExist() {
            //given
            when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

            //when & then
            assertThatThrownBy(() -> templateService.updateTemplate(templateId, request, customUser))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.TEMPLATE_NOT_FOUND));
        }

        @Test
        @DisplayName("템플릿 업데이트 실패 - 존재하지 않는 사용자")
        public void templateUpdateFailsWhenUserDoesNotExist() {
            //given
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
            when(memberService.getMemberByIdOrThrow(memberId)).thenThrow(new ApiException(ErrorCode.MEMBER_NOT_FOUND));

            //when & then
            assertThatThrownBy(() -> templateService.updateTemplate(templateId, request, customUser))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.MEMBER_NOT_FOUND));
        }

        @Test
        @DisplayName("템플릿 업데이트 실패 - 존재하지 않는 1차 카테고리")
        public void templateUpdateFailsWhenFirstCategoryDoesNotExist() {
            //given
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
            when(memberService.getMemberByIdOrThrow(memberId)).thenReturn(existingMember);

            when(categoryService.getFirstCategoryOrThrow(newFirstCategory.getName())).
                    thenThrow(new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST));

            //when & then
            assertThatThrownBy(() -> templateService.updateTemplate(templateId, request, customUser))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.CATEGORY_NOT_FOUND_FIRST));
        }

        @Test
        @DisplayName("템플릿 업데이트 실패 - 존재하지 않는 2차 카테고리")
        public void templateUpdateFailsWhenSecondCategoryDoesNotExist() {
            //given
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
            when(memberService.getMemberByIdOrThrow(memberId)).thenReturn(existingMember);

            when(categoryService.getFirstCategoryOrThrow(newFirstCategory.getName())).thenReturn(newFirstCategory);
            when(categoryService.getSecondCategoryOrThrow(newSecondCategory.getName(), newFirstCategory)).
                    thenThrow(new ApiException(ErrorCode.CATEGORY_NOT_FOUND_SECOND));

            //when & then
            assertThatThrownBy(() -> templateService.updateTemplate(templateId, request, customUser))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.CATEGORY_NOT_FOUND_SECOND));

        }
    }

    @Nested
    @DisplayName("템플릿 삭제 테스트")
    class TemplateDeleteTests {

        private TemplateDeleteRequest request;
        private List<Long> attachmentsToDelete;
        private List<Template> templates;
        private List<TemplateAttachment> templateAttachments;

        @BeforeEach
        public void setUp() {
            Attachment attachment = new Attachment("attachment1");
            TemplateAttachment templateAttachment = new TemplateAttachment(template, attachment);
            setId(attachment, 1L);
            setId(templateAttachment, 1L);

            request = new TemplateDeleteRequest(List.of(templateId));

            templates = List.of(template);
            templateAttachments = List.of(templateAttachment);
            attachmentsToDelete = templateAttachments.stream()
                    .map(ta -> ta.getAttachment().getId())
                    .toList();
        }

        @Test
        @DisplayName("템플릿 삭제 성공")
        public void templateDeleteSuccess() {
            //given
            when(memberService.getMemberByIdOrThrow(customUser.getId())).thenReturn(existingMember);
            when(templateRepository.findAllByIdAndMember(request.getTemplateIds(), existingMember))
                    .thenReturn(templates);

            when(templateAttachmentRepository.findAllByTemplatesJoinFetch(request.getTemplateIds()))
                    .thenReturn(templateAttachments);

            //when
            TemplateDeleteResponse response = templateService.deleteTemplates(request, customUser);

            //then
            verify(templateAttachmentRepository).deleteByTemplates(templates);
            verify(attachmentService).deleteAttachments(attachmentsToDelete);
            verify(templateRepository).deleteByTemplateIds(request.getTemplateIds());

            assertThat(response.getDeletedTemplates())
                    .map(TemplateIdResponse::getTemplateId)
                    .containsExactlyInAnyOrderElementsOf(request.getTemplateIds());
        }

        @Test
        @DisplayName("템플릭 삭제 실패 - 존재하지 않는 사용자")
        public void templateDeleteFailsWhenUserDoesNotExist() {
            //given
            when(memberService.getMemberByIdOrThrow(customUser.getId())).thenThrow(new ApiException(ErrorCode.MEMBER_NOT_FOUND));

            //when & then
            assertThatThrownBy(() -> templateService.deleteTemplates(request, customUser))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.MEMBER_NOT_FOUND));
        }

        @Test
        @DisplayName("템플릭 삭제 실패 - 다른 사람의 템플릿을 삭제하려는 경우")
        public void templateDeleteFailsWhen() {
            //given
            when(memberService.getMemberByIdOrThrow(customUser.getId())).thenReturn(existingMember);
            when(templateRepository.findAllByIdAndMember(request.getTemplateIds(), existingMember))
                    .thenReturn(List.of());


            //when & then
            assertThatThrownBy(() -> templateService.deleteTemplates(request, customUser))
                    .isInstanceOf(ApiException.class)
                    .matches(e -> ((ApiException) e).getErrorCode().equals(ErrorCode.FORBIDDEN));
        }

    }


}
