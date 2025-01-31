package com.quartz.checkin.service;

import com.quartz.checkin.dto.request.FirstCategoryCreateRequest;
import com.quartz.checkin.dto.request.FirstCategoryUpdateRequest;
import com.quartz.checkin.dto.request.SecondCategoryCreateRequest;
import com.quartz.checkin.dto.response.CategoryResponse;
import com.quartz.checkin.dto.response.FirstCategoryCreateResponse;
import com.quartz.checkin.dto.response.SecondCategoryCreateResponse;
import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAllCategories(Long memberId);
    FirstCategoryCreateResponse createFirstCategory(Long memberId, FirstCategoryCreateRequest request);
    void updateFirstCategory(Long memberId, Long firstCategoryId, FirstCategoryUpdateRequest request);
    void deleteFirstCategory(Long memberId, Long firstCategoryId);
    SecondCategoryCreateResponse createSecondCategory(Long memberId, Long firstCategoryId, SecondCategoryCreateRequest request);
}
