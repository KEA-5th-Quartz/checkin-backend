package com.quartz.checkin.controller;

import com.quartz.checkin.dto.request.MemberRegistrationRequest;
import com.quartz.checkin.dto.response.ApiResponse;
import com.quartz.checkin.dto.response.MemberInfoResponse;
import com.quartz.checkin.dto.response.ProfilePicUpdateResponse;
import com.quartz.checkin.security.CustomUser;
import com.quartz.checkin.security.annotation.Admin;
import com.quartz.checkin.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Admin
    @PostMapping
    public ApiResponse<Void> memberRegistration(
            @RequestBody @Valid MemberRegistrationRequest memberRegistrationRequest) {
        memberService.register(memberRegistrationRequest);

        return ApiResponse.createSuccessResponse(HttpStatus.CREATED.value());
    }

    @GetMapping("/{memberId}")
    public ApiResponse<MemberInfoResponse> memberInfo(@PathVariable(name = "memberId") Long memberId) {
        MemberInfoResponse response = memberService.getMemberInfo(memberId);

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @PutMapping("/{memberId}/profile-pic")
    public ApiResponse<ProfilePicUpdateResponse> updateProfilePic(@PathVariable(name = "memberId") Long memberId,
                                                                  @RequestParam(name = "file") MultipartFile file,
                                                                  @AuthenticationPrincipal CustomUser customUser) {
        ProfilePicUpdateResponse response = new ProfilePicUpdateResponse(
                memberService.updateMemberProfilePic(memberId, customUser, file));

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

}
