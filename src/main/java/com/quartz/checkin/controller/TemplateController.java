package com.quartz.checkin.controller;

import com.quartz.checkin.config.S3Config;
import com.quartz.checkin.dto.common.request.SimplePageRequest;
import com.quartz.checkin.dto.common.response.ApiResponse;
import com.quartz.checkin.dto.common.response.UploadAttachmentsResponse;
import com.quartz.checkin.dto.template.request.TemplateDeleteRequest;
import com.quartz.checkin.dto.template.request.TemplateSaveRequest;
import com.quartz.checkin.dto.template.response.TemplateCreateResponse;
import com.quartz.checkin.dto.template.response.TemplateDeleteResponse;
import com.quartz.checkin.dto.template.response.TemplateDetailResponse;
import com.quartz.checkin.dto.template.response.TemplateListResponse;
import com.quartz.checkin.security.CustomUser;
import com.quartz.checkin.security.annotation.User;
import com.quartz.checkin.service.AttachmentService;
import com.quartz.checkin.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    @Operation(summary = "API 명세서 v0.3 line 24", description = "사용자가 템플릿 생성")
    @PostMapping("/templates")
    public ApiResponse<TemplateCreateResponse> createTemplate(
            @RequestBody @Valid TemplateSaveRequest templateSaveRequest,
            @AuthenticationPrincipal CustomUser customUser) {
        TemplateCreateResponse response = templateService.createTemplate(templateSaveRequest, customUser);

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @User
    @Operation(summary = "API 명세서 v0.3 line 27", description = "사용자가 템플릿 다중 삭제")
    @DeleteMapping("/templates")
    public ApiResponse<TemplateDeleteResponse> deleteTemplates(
            @RequestBody @Valid TemplateDeleteRequest templateDeleteRequest,
            @AuthenticationPrincipal CustomUser customUser) {
        TemplateDeleteResponse response = templateService.deleteTemplates(templateDeleteRequest, customUser);

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @User
    @Operation(summary = "API 명세서 v0.3 line 25", description = "사용자가 템플릿에 들어갈 첨부파일 등록")
    @PostMapping("/templates/attachment")
    public ApiResponse<List<UploadAttachmentsResponse>> uploadAttachments(
            @RequestPart(name = "files") List<MultipartFile> multipartFiles) {
        List<UploadAttachmentsResponse> response =
                attachmentService.uploadAttachments(multipartFiles, S3Config.TEMPLATE_DIR);

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @User
    @Operation(summary = "API 명세서 v0.3 line 26", description = "사용자가 템플릿 수정")
    @PutMapping("/templates/{templateId}")
    public ApiResponse<Void> updateTemplate(
            @PathVariable Long templateId,
            @RequestBody @Valid TemplateSaveRequest templateSaveRequest,
            @AuthenticationPrincipal CustomUser customUser) {
        templateService.updateTemplate(templateId, templateSaveRequest, customUser);

        return ApiResponse.createSuccessResponse(HttpStatus.OK.value());
    }

    @User
    @Operation(summary = "API 명세서 v0.3 line 28", description = "사용자가 템플릿 단건 조회, 첨부파일 포함")
    @GetMapping("/templates/{templateId}")
    public ApiResponse<TemplateDetailResponse> template(
            @PathVariable Long templateId,
            @AuthenticationPrincipal CustomUser customUser) {
        TemplateDetailResponse response = templateService.readTemplate(templateId, customUser);

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @User
    @Operation(summary = "API 명세서 v0.3 line 29", description = "사용자의 템플릿 목록 조회, 첨부파일의 경우 단건조회에서 확인")
    @GetMapping("/{memberId}/templates")
    public ApiResponse<TemplateListResponse> templates(
            @PathVariable Long memberId,
            @ModelAttribute @Valid SimplePageRequest pageRequest,
            @AuthenticationPrincipal CustomUser customUser) {
        TemplateListResponse response = templateService.readTemplates(memberId, pageRequest, customUser);

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

}
