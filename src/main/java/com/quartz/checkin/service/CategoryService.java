package com.quartz.checkin.service;

import com.quartz.checkin.dto.category.request.FirstCategoryCreateRequest;
import com.quartz.checkin.dto.category.request.FirstCategoryUpdateRequest;
import com.quartz.checkin.dto.category.request.SecondCategoryCreateRequest;
import com.quartz.checkin.dto.category.request.SecondCategoryUpdateRequest;
import com.quartz.checkin.dto.category.response.CategoryResponse;
import com.quartz.checkin.dto.category.response.FirstCategoryCreateResponse;
import com.quartz.checkin.dto.category.response.SecondCategoryCreateResponse;
import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAllCategories(Long memberId);
    FirstCategoryCreateResponse createFirstCategory(Long memberId, FirstCategoryCreateRequest request);
    void updateFirstCategory(Long memberId, Long firstCategoryId, FirstCategoryUpdateRequest request);
    void deleteFirstCategory(Long memberId, Long firstCategoryId);
    SecondCategoryCreateResponse createSecondCategory(Long memberId, Long firstCategoryId, SecondCategoryCreateRequest request);
    void updateSecondCategory(Long memberId, Long firstCategoryId, Long secondCategoryId, SecondCategoryUpdateRequest request);
    void deleteSecondCategory(Long memberId, Long firstCategoryId, Long secondCategoryId);
    }
