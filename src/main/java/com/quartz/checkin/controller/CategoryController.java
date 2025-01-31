package com.quartz.checkin.controller;

import com.quartz.checkin.dto.response.ApiResponse;
import com.quartz.checkin.dto.response.CategoryResponse;
import com.quartz.checkin.security.CustomUser;
import com.quartz.checkin.security.annotation.Admin;
import com.quartz.checkin.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Admin
    @Operation(summary = "API 명세서 v0.2 line 51", description = "관리자가 카테고리 조회")
    @GetMapping
    public ApiResponse<List<CategoryResponse>> getAllCategories(
            @AuthenticationPrincipal CustomUser user) {
        List<CategoryResponse> response = categoryService.getAllCategories(user.getId());
        return ApiResponse.createSuccessResponseWithData(HttpStatus.OK.value(), response);
    }

}
