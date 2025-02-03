package com.quartz.checkin.controller;

import com.quartz.checkin.dto.auth.response.AuthenticationResponse;
import com.quartz.checkin.dto.common.response.ApiResponse;
import com.quartz.checkin.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "API 명세서 v0.2 line 5", description = "refreshToken 재발급")
    @PostMapping("/refresh")
    public ApiResponse<AuthenticationResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        AuthenticationResponse authenticationResponse = authService.refresh(request, response);

        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), authenticationResponse);
    }
}
