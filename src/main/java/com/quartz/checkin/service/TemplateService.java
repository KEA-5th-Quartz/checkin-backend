package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.request.TemplateSaveRequest;
import com.quartz.checkin.dto.response.TemplateCreateResponse;
import com.quartz.checkin.dto.response.TemplateResponse;
import com.quartz.checkin.dto.response.UploadAttachmentsResponse;
import com.quartz.checkin.entity.Attachment;
import com.quartz.checkin.entity.Category;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Template;
import com.quartz.checkin.entity.TemplateAttachment;
import com.quartz.checkin.repository.AttachmentRepository;
import com.quartz.checkin.repository.TemplateAttachmentRepository;
import com.quartz.checkin.repository.TemplateRepository;
import com.quartz.checkin.security.CustomUser;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TemplateService {

    private final AttachmentRepository attachmentRepository;
    private final CategoryService categoryService;
    private final MemberService memberService;
    private final TemplateAttachmentRepository templateAttachmentRepository;
    private final TemplateRepository templateRepository;

    public Template getTemplateByIdOrThrow(Long templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 템플릿입니다.");
                    return new ApiException(ErrorCode.TEMPLATE_NOT_FOUND);
                });
    }

    public TemplateResponse readTemplate(Long templateId, CustomUser customUser) {
        Template template = templateRepository.findByIdJoinFetch(templateId)
                .orElseThrow(() -> new ApiException(ErrorCode.TEMPLATE_NOT_FOUND));

        Member member = memberService.getMemberByIdOrThrow(customUser.getId());

        checkTemplateOwner(template, member);

        List<TemplateAttachment> templateAttachments =
                templateAttachmentRepository.findAllByTemplateJoinFetch(template);

        List<UploadAttachmentsResponse> attachmentsResponses = templateAttachments.stream()
                .map(ta -> new UploadAttachmentsResponse(ta.getAttachment().getId(), ta.getAttachment().getUrl()))
                .toList();

        return TemplateResponse.builder()
                .id(template.getId())
                .title(template.getTitle())
                .firstCategory(template.getFirstCategory().getName())
                .secondCategory(template.getSecondCategory().getName())
                .content(template.getContent())
                .attachmentIds(attachmentsResponses)
                .build();

    }

    @Transactional
    public TemplateCreateResponse createTemplate(TemplateSaveRequest templateSaveRequest, CustomUser customUser) {
        Member member = memberService.getMemberByIdOrThrow(customUser.getId());

        Category firstCategory = categoryService.getFirstCategoryOrThrow(templateSaveRequest.getFirstCategory());
        Category secondCategory = categoryService.
                getSecondCategoryOrThrow(templateSaveRequest.getSecondCategory(), firstCategory);

        List<Long> attachmentIds = templateSaveRequest.getAttachmentIds();
        List<Attachment> attachments = attachmentRepository.findAllById(attachmentIds);

        checkInvalidAttachment(attachmentIds, attachments);

        Template template = templateRepository.save(Template.builder()
                .title(templateSaveRequest.getTitle())
                .member(member)
                .firstCategory(firstCategory)
                .secondCategory(secondCategory)
                .content(templateSaveRequest.getContent())
                .build());

        List<TemplateAttachment> templateAttachments = new ArrayList<>();
        for (Attachment attachment : attachments) {
            templateAttachments.add(new TemplateAttachment(template, attachment));
        }

        templateAttachmentRepository.saveAll(templateAttachments);

        return new TemplateCreateResponse(template.getId());
    }

    @Transactional
    public void updateTemplate(Long templateId, TemplateSaveRequest templateSaveRequest, CustomUser customUser) {
        Template template = getTemplateByIdOrThrow(templateId);

        Member member = memberService.getMemberByIdOrThrow(customUser.getId());

        checkTemplateOwner(template, member);

        Category firstCategory = categoryService.getFirstCategoryOrThrow(templateSaveRequest.getFirstCategory());
        Category secondCategory = categoryService.
                getSecondCategoryOrThrow(templateSaveRequest.getSecondCategory(), firstCategory);

        List<Long> newAttachmentIds = templateSaveRequest.getAttachmentIds();
        List<Attachment> newAttachments = attachmentRepository.findAllById(newAttachmentIds);

        checkInvalidAttachment(newAttachmentIds, newAttachments);

        template.updateTitle(templateSaveRequest.getTitle());
        template.updateContent(templateSaveRequest.getContent());
        template.updateCategories(firstCategory, secondCategory);

        // 해당 템플릿의 저장된 첨부파일 ID들
        List<Long> savedAttachmentIds = templateAttachmentRepository.findByTemplate(template)
                .stream()
                .map(ta -> ta.getAttachment().getId())
                .toList();

        // 추가해야 할 첨부파일 ID들
        List<Long> attachmentIdsToAdd = newAttachmentIds.stream()
                .filter(id -> !savedAttachmentIds.contains(id))
                .toList();

        // 제거해야 할 첨부파일 ID들
        List<Long> attachmentIdsToRemove = savedAttachmentIds.stream()
                .filter(id -> !newAttachmentIds.contains(id))
                .toList();

        // 추가해야 할 첨부파일
        List<Attachment> attachmentsToAdd = newAttachments.stream()
                .filter(a -> attachmentIdsToAdd.contains(a.getId()))
                .toList();

        List<TemplateAttachment> newTemplateAttachments = attachmentsToAdd.stream()
                .map(a -> new TemplateAttachment(template, a))
                .toList();

        templateAttachmentRepository.saveAll(newTemplateAttachments);

        // 해당 첨부파일이 티켓에서 사용 중일 수 있기에, 중간 테이블에서만 삭제
        if (!attachmentIdsToRemove.isEmpty()) {
            templateAttachmentRepository.deleteByTemplateAndAttachmentIds(template, attachmentIdsToRemove);
        }

    }

    private void checkInvalidAttachment(List<Long> attachmentIds, List<Attachment> attachments) {
        if (attachmentIds.size() != attachments.size()) {
            log.error("존재하지 않는 첨부파일이 포함되어 있습니다.");
            throw new ApiException(ErrorCode.INVALID_TEMPLATE_ATTACHMENT_IDS);
        }
    }

    private void checkTemplateOwner(Template template, Member member) {
        if (!template.getMember().getId().equals(member.getId())) {
            log.error("다른 사용자의 리소스에 접근하려고 합니다.");
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }


}
