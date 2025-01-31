package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.category.request.FirstCategoryCreateRequest;
import com.quartz.checkin.dto.category.request.FirstCategoryUpdateRequest;
import com.quartz.checkin.dto.category.request.SecondCategoryCreateRequest;
import com.quartz.checkin.dto.category.request.SecondCategoryUpdateRequest;
import com.quartz.checkin.dto.category.response.CategoryResponse;
import com.quartz.checkin.dto.category.response.FirstCategoryCreateResponse;
import com.quartz.checkin.dto.category.response.SecondCategoryCreateResponse;
import com.quartz.checkin.dto.category.response.SecondCategoryResponse;
import com.quartz.checkin.entity.Category;
import com.quartz.checkin.repository.CategoryRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(Long memberId) {
        List<Object[]> result = categoryRepository.findAllCategoriesWithSecondCategories();
        Map<Long, CategoryResponse> categoryMap = new LinkedHashMap<>();

        for (Object[] row : result) {
            Category firstCategory = (Category) row[0];
            Category secondCategory = (Category) row[1];

            categoryMap.putIfAbsent(firstCategory.getId(),
                    new CategoryResponse(firstCategory.getId(), firstCategory.getName(), new ArrayList<>()));

            if (secondCategory != null) {
                categoryMap.get(firstCategory.getId()).getSecondCategories()
                        .add(new SecondCategoryResponse(secondCategory.getId(), secondCategory.getName()));
            }
        }

        return new ArrayList<>(categoryMap.values());
    }

    @Transactional
    public FirstCategoryCreateResponse createFirstCategory(Long memberId, FirstCategoryCreateRequest request) {
        checkDuplicateFirstCategory(request.getName());
        Category firstCategory = new Category(null, request.getName());
        categoryRepository.save(firstCategory);
        return new FirstCategoryCreateResponse(firstCategory.getId());
    }

    @Transactional
    public void updateFirstCategory(Long memberId, Long firstCategoryId, FirstCategoryUpdateRequest request) {
        Category firstCategory = getValidFirstCategory(firstCategoryId);
        checkDuplicateFirstCategory(request.getFirstCategory());

        firstCategory.updateName(request.getFirstCategory());
    }

    @Transactional
    public void deleteFirstCategory(Long memberId, Long firstCategoryId) {
        Category firstCategory = getValidFirstCategory(firstCategoryId);

        if (categoryRepository.existsByParent(firstCategory)) {
            throw new ApiException(ErrorCode.CATEGORY_HAS_SUBCATEGORIES);
        }

        categoryRepository.delete(firstCategory);
    }

    @Transactional
    public SecondCategoryCreateResponse createSecondCategory(Long memberId, Long firstCategoryId, SecondCategoryCreateRequest request) {
        Category firstCategory = getValidFirstCategory(firstCategoryId);
        checkDuplicateSecondCategory(request.getName(), firstCategory);

        Category secondCategory = new Category(firstCategory, request.getName());
        categoryRepository.save(secondCategory);

        return new SecondCategoryCreateResponse(secondCategory.getId());
    }

    @Transactional
    public void updateSecondCategory(Long memberId, Long firstCategoryId, Long secondCategoryId, SecondCategoryUpdateRequest request) {
        Category secondCategory = getValidSecondCategory(firstCategoryId, secondCategoryId);
        checkDuplicateSecondCategory(request.getSecondCategory(), secondCategory.getParent());

        secondCategory.updateName(request.getSecondCategory());
    }

    @Transactional
    public void deleteSecondCategory(Long memberId, Long firstCategoryId, Long secondCategoryId) {
        Category secondCategory = getValidSecondCategory(firstCategoryId, secondCategoryId);
        categoryRepository.delete(secondCategory);
    }

    // 1차 카테고리 조회 (없으면 예외)
    private Category getValidFirstCategory(Long firstCategoryId) {
        Category firstCategory = categoryRepository.findById(firstCategoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST));

        if (firstCategory.getParent() != null) {
            throw new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST);
        }

        return firstCategory;
    }

    // 2차 카테고리 조회 (없으면 예외)
    private Category getValidSecondCategory(Long firstCategoryId, Long secondCategoryId) {
        Category secondCategory = categoryRepository.findById(secondCategoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_SECOND));

        if (secondCategory.getParent() == null || !secondCategory.getParent().getId().equals(firstCategoryId)) {
            throw new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST);
        }

        return secondCategory;
    }

    // 중복된 1차 카테고리 확인
    private void checkDuplicateFirstCategory(String name) {
        if (categoryRepository.existsByNameAndParentIsNull(name)) {
            throw new ApiException(ErrorCode.DUPLICATE_CATEGORY_FIRST);
        }
    }

    // 중복된 2차 카테고리 확인
    private void checkDuplicateSecondCategory(String name, Category parent) {
        if (categoryRepository.existsByNameAndParent(name, parent)) {
            throw new ApiException(ErrorCode.DUPLICATE_CATEGORY_SECOND);
        }
    }
}
