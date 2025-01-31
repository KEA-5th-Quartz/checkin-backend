package com.quartz.checkin.service;

import com.quartz.checkin.dto.request.CategoryCreateRequest;
import com.quartz.checkin.dto.response.CategoryCreateResponse;
import com.quartz.checkin.dto.response.CategoryResponse;
import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAllCategories(Long memberId);
    CategoryCreateResponse createFirstCategory(Long memberId, CategoryCreateRequest request);
}
