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
                    new CategoryResponse(firstCategory.getId(), firstCategory.getName(),
                            firstCategory.getAlias(), firstCategory.getContentGuide(), new ArrayList<>()));

            if (secondCategory != null) {
                categoryMap.get(firstCategory.getId()).getSecondCategories()
                        .add(new SecondCategoryResponse(secondCategory.getId(), secondCategory.getName()));
            }
        }

        return new ArrayList<>(categoryMap.values());
    }

    @Transactional
    public FirstCategoryCreateResponse createFirstCategory(Long memberId, FirstCategoryCreateRequest request) {
        checkDuplicateFirstCategory(request.getName(), null);
        checkDuplicateAlias(request.getAlias(), null);

        // Alias 자동 변환 로직
        String formattedAlias = formatAlias(request.getAlias());

        // 카테고리 생성 및 저장
        Category firstCategory = new Category(null, request.getName(), formattedAlias, request.getContentGuide());
        categoryRepository.save(firstCategory);

        return new FirstCategoryCreateResponse(firstCategory.getId());
    }

    @Transactional
    public void updateFirstCategory(Long memberId, Long firstCategoryId, FirstCategoryUpdateRequest request) {
        Category firstCategory = getValidFirstCategory(firstCategoryId);

        // 본인의 기존 카테고리 이름과 다를 경우에만 중복 검사 수행
        if (!firstCategory.getName().equals(request.getName())) {
            checkDuplicateFirstCategory(request.getName(), firstCategoryId);
        }

        // 본인의 기존 약어와 다를 경우에만 중복 검사 수행
        if (!firstCategory.getAlias().equals(request.getAlias())) {
            checkDuplicateAlias(request.getAlias(), firstCategoryId);
        }

        firstCategory.updateCategory(request.getName(), request.getAlias(), request.getContentGuide());
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
        checkDuplicateSecondCategory(request.getName(), firstCategory, null);

        Category secondCategory = new Category(firstCategory, request.getName(), null, null);
        categoryRepository.save(secondCategory);

        return new SecondCategoryCreateResponse(secondCategory.getId());
    }

    @Transactional
    public void updateSecondCategory(Long memberId, Long firstCategoryId, Long secondCategoryId, SecondCategoryUpdateRequest request) {
        Category secondCategory = getValidSecondCategory(firstCategoryId, secondCategoryId);

        // 본인의 기존 카테고리 이름과 다를 경우에만 중복 검사 수행
        if (!secondCategory.getName().equals(request.getSecondCategory())) {
            checkDuplicateSecondCategory(request.getSecondCategory(), secondCategory.getParent(), secondCategoryId);
        }

        secondCategory.updateCategory(request.getSecondCategory(), null, null);
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

    // 중복된 1차 카테고리 확인 (자기 자신 제외)
    private void checkDuplicateFirstCategory(String name, Long excludeId) {
        if (categoryRepository.existsByNameAndParentIsNullAndIdNot(name, excludeId)) {
            throw new ApiException(ErrorCode.DUPLICATE_CATEGORY_FIRST);
        }
    }

    // 중복된 2차 카테고리 확인 (자기 자신 제외)
    private void checkDuplicateSecondCategory(String name, Category parent, Long excludeId) {
        if (categoryRepository.existsByNameAndParentAndIdNot(name, parent, excludeId)) {
            throw new ApiException(ErrorCode.DUPLICATE_CATEGORY_SECOND);
        }
    }

    public Category getFirstCategoryOrThrow(String firstCategory) {
        return categoryRepository.findByNameAndParentIsNull(firstCategory)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST));
    }

    public Category getSecondCategoryOrThrow(String secondCategory, Category firstCategory) {
        return categoryRepository.findByNameAndParent(secondCategory, firstCategory)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_SECOND));
    }

    // 중복된 Alias 확인 (자기 자신 제외)
    private void checkDuplicateAlias(String alias, Long excludeId) {
        if (categoryRepository.existsByAliasAndIdNot(alias, excludeId)) {
            throw new ApiException(ErrorCode.DUPLICATE_ALIAS);
        }
    }

    private String formatAlias(String alias) {
        if (alias.length() == 2) {
            return alias + "__";
        } else if (alias.length() == 3) {
            return alias + "_";
        } else if (alias.length() == 4) {
            return alias;  // 4글자는 그대로 사용
        } else {
            throw new ApiException(ErrorCode.INVALID_ALIAS_FORMAT);
        }
    }

}
