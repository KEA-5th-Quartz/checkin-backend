package com.quartz.checkin.controller;

import com.quartz.checkin.dto.request.MemberRegistrationRequest;
import com.quartz.checkin.dto.response.ApiResponse;
import com.quartz.checkin.security.annotation.Admin;
import com.quartz.checkin.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Admin
    @PostMapping
    public ApiResponse<Void> memberRegistration(@RequestBody @Valid MemberRegistrationRequest memberRegistrationRequest) {
        memberService.register(memberRegistrationRequest);

        return ApiResponse.createSuccessResponse(HttpStatus.CREATED.value());
    }

}
