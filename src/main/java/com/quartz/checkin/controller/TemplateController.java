package com.quartz.checkin.controller;

import com.quartz.checkin.dto.request.TemplateCreateRequest;
import com.quartz.checkin.dto.response.ApiResponse;
import com.quartz.checkin.dto.response.TemplateCreateResponse;
import com.quartz.checkin.dto.response.UploadAttachmentsResponse;
import com.quartz.checkin.security.CustomUser;
import com.quartz.checkin.security.annotation.User;
import com.quartz.checkin.service.TemplateService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
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

    @User
    @PostMapping("/templates")
    public ApiResponse<TemplateCreateResponse> createTemplate(
            @RequestBody @Valid TemplateCreateRequest templateCreateRequest,
            @AuthenticationPrincipal CustomUser customUser) {
        TemplateCreateResponse response = templateService.createTemplate(templateCreateRequest, customUser);

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @User
    @PostMapping("/templates/attachment")
    public ApiResponse<UploadAttachmentsResponse> uploadAttachments(
            @RequestPart(name = "files") List<MultipartFile> multipartFiles) {
        UploadAttachmentsResponse response = templateService.uploadAttachments(multipartFiles);

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }
}
