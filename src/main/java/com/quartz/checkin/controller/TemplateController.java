package com.quartz.checkin.controller;

import com.quartz.checkin.config.S3Config;
import com.quartz.checkin.dto.request.SimplePageRequest;
import com.quartz.checkin.dto.request.TemplateSaveRequest;
import com.quartz.checkin.dto.response.ApiResponse;
import com.quartz.checkin.dto.response.TemplateCreateResponse;
import com.quartz.checkin.dto.response.TemplateDetailResponse;
import com.quartz.checkin.dto.response.TemplateListResponse;
import com.quartz.checkin.dto.response.UploadAttachmentsResponse;
import com.quartz.checkin.security.CustomUser;
import com.quartz.checkin.security.annotation.User;
import com.quartz.checkin.service.AttachmentService;
import com.quartz.checkin.service.TemplateService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;
    private final AttachmentService attachmentService;

    @User
    @PostMapping("/templates")
    public ApiResponse<TemplateCreateResponse> createTemplate(
            @RequestBody @Valid TemplateSaveRequest templateSaveRequest,
            @AuthenticationPrincipal CustomUser customUser) {
        TemplateCreateResponse response = templateService.createTemplate(templateSaveRequest, customUser);

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @User
    @PostMapping("/templates/attachment")
    public ApiResponse<List<UploadAttachmentsResponse>> uploadAttachments(
            @RequestPart(name = "files") List<MultipartFile> multipartFiles) {
        List<UploadAttachmentsResponse> response =
                attachmentService.uploadAttachments(multipartFiles, S3Config.TEMPLATE_DIR);

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @User
    @PutMapping("/templates/{templateId}")
    public ApiResponse<Void> updateTemplate(
            @PathVariable Long templateId,
            @RequestBody @Valid TemplateSaveRequest templateSaveRequest,
            @AuthenticationPrincipal CustomUser customUser) {
        templateService.updateTemplate(templateId, templateSaveRequest, customUser);

        return ApiResponse.createSuccessResponse(HttpStatus.OK.value());
    }

    @User
    @GetMapping("/templates/{templateId}")
    public ApiResponse<TemplateDetailResponse> template(
            @PathVariable Long templateId,
            @AuthenticationPrincipal CustomUser customUser) {
        TemplateDetailResponse response = templateService.readTemplate(templateId, customUser);

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @User
    @GetMapping("/{memberId}/templates")
    public ApiResponse<TemplateListResponse> templates(
            @PathVariable Long memberId,
            @ModelAttribute @Valid SimplePageRequest pageRequest,
            @AuthenticationPrincipal CustomUser customUser) {
        TemplateListResponse response = templateService.readTemplates(memberId, pageRequest, customUser);

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

}
