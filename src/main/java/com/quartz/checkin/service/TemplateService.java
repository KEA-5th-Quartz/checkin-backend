package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.config.S3Config;
import com.quartz.checkin.dto.request.TemplateCreateRequest;
import com.quartz.checkin.dto.response.TemplateCreateResponse;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TemplateService {

    private final AttachmentRepository attachmentRepository;
    private final CategoryService categoryService;
    private final MemberService memberService;
    private final S3UploadService s3UploadService;
    private final TemplateAttachmentRepository templateAttachmentRepository;
    private final TemplateRepository templateRepository;

    @Transactional
    public TemplateCreateResponse createTemplate(TemplateCreateRequest templateCreateRequest, CustomUser customUser) {
        Member member = memberService.getMemberByIdOrThrow(customUser.getId());

        Category firstCategory = categoryService.getFirstCategoryOrThrow(templateCreateRequest.getFirstCategory());
        Category secondCategory = categoryService.
                getSecondCategoryOrThrow(templateCreateRequest.getSecondCategory(), firstCategory);

        List<Long> attachmentIds = templateCreateRequest.getAttachmentIds();
        List<Attachment> attachments = attachmentRepository.findAllById(attachmentIds);

        if (attachments.size() != attachmentIds.size()) {
            log.error("존재하지 않는 첨부파일이 포함되어 있습니다.");
            throw new ApiException(ErrorCode.INVALID_TEMPLATE_ATTACHMENT_IDS);
        }

        Template template = templateRepository.save(Template.builder()
                .title(templateCreateRequest.getTitle())
                .member(member)
                .firstCategory(firstCategory)
                .secondCategory(secondCategory)
                .content(templateCreateRequest.getContent())
                .build());

        List<TemplateAttachment> templateAttachments = new ArrayList<>();
        for (Attachment attachment : attachments) {
            templateAttachments.add(new TemplateAttachment(template, attachment));
        }

        templateAttachmentRepository.saveAll(templateAttachments);

        return new TemplateCreateResponse(template.getId());
    }

    @Transactional
    public UploadAttachmentsResponse uploadAttachments(List<MultipartFile> multipartFiles) {

        for (MultipartFile multipartFile : multipartFiles) {
            if (multipartFile.isEmpty()) {
                log.error("첨부된 파일을 찾을 수 없습니다. {}", multipartFile.getOriginalFilename());
                throw new ApiException(ErrorCode.INVALID_DATA);
            }
        }

        List<Attachment> attachments = new ArrayList<>();
        for (MultipartFile multipartFile : multipartFiles) {
            try {
                String url = s3UploadService.uploadFile(multipartFile, S3Config.TEMPLATE_DIR);
                attachments.add(new Attachment(url));
            } catch (IOException exception) {
                log.error("S3에 파일을 업로드할 수 없습니다. {}", exception.getMessage());
                throw new ApiException(ErrorCode.OBJECT_STORAGE_ERROR);
            }
        }

        List<Long> attachmentIds = attachmentRepository.saveAll(attachments)
                .stream()
                .map(Attachment::getId)
                .toList();

        return new UploadAttachmentsResponse(attachmentIds);

    }

}
