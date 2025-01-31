package com.quartz.checkin.controller;

import com.quartz.checkin.dto.response.ApiResponse;
import com.quartz.checkin.dto.response.UploadAttachmentsResponse;
import com.quartz.checkin.security.annotation.User;
import com.quartz.checkin.service.TemplateService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
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
    @PostMapping("/templates/attachment")
    public ApiResponse<UploadAttachmentsResponse> uploadAttachments(@RequestPart(name = "files") List<MultipartFile> multipartFiles) {
        UploadAttachmentsResponse response = templateService.uploadAttachments(multipartFiles);

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }
}
