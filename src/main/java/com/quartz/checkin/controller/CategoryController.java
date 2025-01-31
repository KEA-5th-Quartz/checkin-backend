package com.quartz.checkin.controller;

import com.quartz.checkin.dto.request.FirstCategoryCreateRequest;
import com.quartz.checkin.dto.request.FirstCategoryUpdateRequest;
import com.quartz.checkin.dto.request.SecondCategoryCreateRequest;
import com.quartz.checkin.dto.request.SecondCategoryUpdateRequest;
import com.quartz.checkin.dto.response.ApiResponse;
import com.quartz.checkin.dto.response.CategoryResponse;
import com.quartz.checkin.dto.response.FirstCategoryCreateResponse;
import com.quartz.checkin.dto.response.SecondCategoryCreateResponse;
import com.quartz.checkin.security.CustomUser;
import com.quartz.checkin.security.annotation.Admin;
import com.quartz.checkin.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @Admin
    @Operation(summary = "API 명세서 v0.2 line 52", description = "관리자가 1차 카테고리 작성")
    @PostMapping
    public ApiResponse<FirstCategoryCreateResponse> createFirstCategory(
            @RequestBody @Valid FirstCategoryCreateRequest request,
            @AuthenticationPrincipal CustomUser user) {

        FirstCategoryCreateResponse response = categoryService.createFirstCategory(user.getId(),request);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.CREATED.value(), response);
    }

    @Admin
    @Operation(summary = "API 명세서 v0.2 line 53", description = "관리자가 1차 카테고리 수정")
    @PutMapping("/{firstCategoryId}")
    public ApiResponse<Void> updateFirstCategory(
            @PathVariable Long firstCategoryId,
            @RequestBody @Valid FirstCategoryUpdateRequest request,
            @AuthenticationPrincipal CustomUser user) {

        categoryService.updateFirstCategory(user.getId(),firstCategoryId, request);
        return ApiResponse.createSuccessResponse(HttpStatus.OK.value());
    }

    @Admin
    @Operation(summary = "API 명세서 v0.2 line 54", description = "관리자가 1차 카테고리 삭제")
    @DeleteMapping("/{firstCategoryId}")
    public ApiResponse<Void> deleteFirstCategory(
            @PathVariable Long firstCategoryId,
            @AuthenticationPrincipal CustomUser user) {
        categoryService.deleteFirstCategory(user.getId(), firstCategoryId);
        return ApiResponse.createSuccessResponse(HttpStatus.OK.value());
    }

    @Admin
    @Operation(summary = "API 명세서 v0.2 line 55", description = "관리자가 2차 카테고리 작성")
    @PostMapping("/{firstCategoryId}/second-categories")
    public ApiResponse<SecondCategoryCreateResponse> createSecondCategory(
            @PathVariable Long firstCategoryId,
            @RequestBody @Valid SecondCategoryCreateRequest request,
            @AuthenticationPrincipal CustomUser user) {

        SecondCategoryCreateResponse response = categoryService.createSecondCategory(user.getId(),firstCategoryId,request);
        return ApiResponse.createSuccessResponseWithData(HttpStatus.CREATED.value(), response);
    }

    @Admin
    @Operation(summary = "API 명세서 v0.2 line 56", description = "관리자가 2차 카테고리 수정")
    @PutMapping("/{firstCategoryId}/second-categories/{secondCategoryId}")
    public ApiResponse<Void> updateSecondCategory(
            @PathVariable Long firstCategoryId,
            @PathVariable Long secondCategoryId,
            @RequestBody @Valid SecondCategoryUpdateRequest request,
            @AuthenticationPrincipal CustomUser user) {

        categoryService.updateSecondCategory(user.getId(), firstCategoryId, secondCategoryId, request);
        return ApiResponse.createSuccessResponse(HttpStatus.OK.value());
    }
}
