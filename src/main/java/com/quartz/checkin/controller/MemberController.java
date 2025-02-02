package com.quartz.checkin.controller;

import com.quartz.checkin.dto.common.response.ApiResponse;
import com.quartz.checkin.dto.member.request.EmailCheckRequest;
import com.quartz.checkin.dto.member.request.MemberInfoListRequest;
import com.quartz.checkin.dto.member.request.MemberRegistrationRequest;
import com.quartz.checkin.dto.member.request.PasswordChangeRequest;
import com.quartz.checkin.dto.member.request.PasswordResetEmailRequest;
import com.quartz.checkin.dto.member.request.PasswordResetRequest;
import com.quartz.checkin.dto.member.request.RoleUpdateRequest;
import com.quartz.checkin.dto.member.request.UsernameCheckRequest;
import com.quartz.checkin.dto.member.response.MemberInfoListResponse;
import com.quartz.checkin.dto.member.response.MemberInfoResponse;
import com.quartz.checkin.dto.member.response.ProfilePicUpdateResponse;
import com.quartz.checkin.security.CustomUser;
import com.quartz.checkin.security.annotation.Admin;
import com.quartz.checkin.security.annotation.AdminOrManager;
import com.quartz.checkin.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
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
    @Operation(summary = "API 명세서 v0.2 line 7", description = "관리자가 사용자 생성")
    @PostMapping
    public ApiResponse<Void> memberRegistration(
            @RequestBody @Valid MemberRegistrationRequest memberRegistrationRequest) {
        memberService.register(memberRegistrationRequest);

        return ApiResponse.createSuccessResponse(HttpStatus.CREATED.value());
    }

    @AdminOrManager
    @Operation(summary = "API 명세서 v0.2 line 8", description = "관리자 또는 담당자가 사용자 목록 조회")
    @GetMapping
    public ApiResponse<MemberInfoListResponse> memberInfoList(
            @ModelAttribute @Valid MemberInfoListRequest memberInfoListRequest) {
        MemberInfoListResponse response = memberService.getMemberInfoList(memberInfoListRequest);

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Operation(summary = "API 명세서 v0.2 line 12", description = "회원 ID로 회원 정보 조회")
    @GetMapping("/{memberId}")
    public ApiResponse<MemberInfoResponse> memberInfo(@PathVariable(name = "memberId") Long memberId) {
        MemberInfoResponse response = memberService.getMemberInfo(memberId);

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Operation(summary = "API 명세서 v0.2 line 19", description = "회원 ID 중복 검사")
    @GetMapping("/check-username")
    public ApiResponse<Void> checkUsernameDuplicate(@ModelAttribute @Valid UsernameCheckRequest usernameCheckRequest) {
        memberService.checkUsernameDuplicate(usernameCheckRequest.getUsername());

        return ApiResponse.createSuccessResponse(HttpStatus.OK.value());
    }

    @Operation(summary = "API 명세서 v0.2 line 20", description = "이메일 중복 검사")
    @GetMapping("/check-email")
    public ApiResponse<Void> checkUsernameDuplicate(@ModelAttribute @Valid EmailCheckRequest emailCheckRequest) {
        memberService.checkEmailDuplicate(emailCheckRequest.getEmail());

        return ApiResponse.createSuccessResponse(HttpStatus.OK.value());
    }

    @Operation(summary = "API 명세서 v0.2 line 15", description = "기존 비밀번호와 새로운 비밀번호를 통해 회원 자신의 비밀번호를 재설정")
    @PutMapping("/{memberId}/password")
    public ApiResponse<Void> updatePassword(@PathVariable(name = "memberId") Long memberId,
                                            @RequestBody @Valid PasswordChangeRequest passwordChangeRequest,
                                            @AuthenticationPrincipal CustomUser customUser) {
        memberService.changeMemberPassword(memberId, customUser, passwordChangeRequest);

        return ApiResponse.createSuccessResponse(HttpStatus.OK.value());
    }

    @Operation(summary = "API 명세서 v0.2 line 13", description = "비밀번호 초기 페이지로 가는 링크가 담긴 메일 전송")
    @PostMapping("/password-reset")
    public ApiResponse<Void> sendPasswordResetMail(
            @RequestBody @Valid PasswordResetEmailRequest passwordResetEmailRequest) {
        memberService.sendPasswordResetMail(passwordResetEmailRequest);

        return ApiResponse.createSuccessResponse(HttpStatus.OK.value());
    }

    @Operation(summary = "API 명세서 v0.2 line 14", description = "비밀번호 초기화 토큰과 새 비밀번호로 비밀번호를 초기화")
    @PutMapping("/{memberId}/password-reset")
    public ApiResponse<Void> resetPassword(@PathVariable(name = "memberId") Long memberId,
                                           @RequestBody @Valid PasswordResetRequest passwordResetRequest) {
        memberService.resetMemberPassword(memberId, passwordResetRequest);

        return ApiResponse.createSuccessResponse(HttpStatus.OK.value());
    }


    @Operation(summary = "API 명세서 v0.2 line 16", description = "회원의 프로필 사진 변경")
    @PutMapping("/{memberId}/profile-pic")
    public ApiResponse<ProfilePicUpdateResponse> updateProfilePic(@PathVariable(name = "memberId") Long memberId,
                                                                  @RequestParam(name = "file") MultipartFile file,
                                                                  @AuthenticationPrincipal CustomUser customUser) {
        ProfilePicUpdateResponse response = new ProfilePicUpdateResponse(
                memberService.updateMemberProfilePic(memberId, customUser, file));

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

    @Admin
    @Operation(summary = "API 명세서 v0.2 line 17", description = "관리자가 회원의 권한 변경")
    @PutMapping("/{memberId}/role")
    public ApiResponse<Void> updateRole(@PathVariable(name = "memberId") Long memberId,
                                        @RequestBody @Valid RoleUpdateRequest roleUpdateRequest) {
        memberService.updateMemberRole(memberId, roleUpdateRequest);

        return ApiResponse.createSuccessResponse(HttpStatus.OK.value());
    }

}
