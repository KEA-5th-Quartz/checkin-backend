package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.request.FirstCategoryCreateRequest;
import com.quartz.checkin.dto.request.FirstCategoryUpdateRequest;
import com.quartz.checkin.dto.request.SecondCategoryCreateRequest;
import com.quartz.checkin.dto.request.SecondCategoryUpdateRequest;
import com.quartz.checkin.dto.response.CategoryResponse;
import com.quartz.checkin.dto.response.FirstCategoryCreateResponse;
import com.quartz.checkin.dto.response.SecondCategoryCreateResponse;
import com.quartz.checkin.dto.response.SecondCategoryResponse;
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

        // 1차 카테고리 별로 2차 카테고리를 매핑할 Map 생성
        Map<Long, CategoryResponse> categoryMap = new LinkedHashMap<>();

        for (Object[] row : result) {
            Category firstCategory = (Category) row[0];
            Category secondCategory = (Category) row[1];

            // 1차 카테고리를 Map에 저장 (없으면 추가)
            categoryMap.putIfAbsent(firstCategory.getId(),
                    new CategoryResponse(firstCategory.getId(), firstCategory.getName(), new ArrayList<>()));

            // 2차 카테고리가 존재하면 추가
            if (secondCategory != null) {
                categoryMap.get(firstCategory.getId()).getSecondCategories()
                        .add(new SecondCategoryResponse(secondCategory.getId(), secondCategory.getName()));
            }
        }

        return new ArrayList<>(categoryMap.values());
    }

    @Transactional
    public FirstCategoryCreateResponse createFirstCategory(Long memberId, FirstCategoryCreateRequest request) {
        // 동일한 이름의 1차 카테고리 존재 여부 확인
        if (categoryRepository.existsByNameAndParentIsNull(request.getName())) {
            throw new ApiException(ErrorCode.DUPLICATE_CATEGORY_FIRST);
        }

        // 1차 카테고리 생성 (부모 없음)
        Category firstCategory = new Category(null, request.getName());
        categoryRepository.save(firstCategory);

        return new FirstCategoryCreateResponse(firstCategory.getId());
    }

    @Transactional
    public void updateFirstCategory(Long memberId, Long firstCategoryId, FirstCategoryUpdateRequest request) {
        // 존재하지 않는 1차 카테고리 예외 처리
        Category firstCategory = categoryRepository.findById(firstCategoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST));

        // 동일한 이름의 1차 카테고리 존재 여부 확인 (자기 자신은 제외)
        if (categoryRepository.existsByNameAndParentIsNull(request.getFirstCategory()) &&
                !firstCategory.getName().equals(request.getFirstCategory())) {
            throw new ApiException(ErrorCode.DUPLICATE_CATEGORY_FIRST);
        }

        // 1차 카테고리 이름 변경
        firstCategory.updateName(request.getFirstCategory());
    }

    @Transactional
    public void deleteFirstCategory(Long memberId, Long firstCategoryId) {
        // 존재하지 않는 1차 카테고리 확인
        Category firstCategory = categoryRepository.findById(firstCategoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST));

        if (firstCategory.getParent() != null) {
            throw new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST);
        }

        // 하위 2차 카테고리 존재 여부 확인
        if (categoryRepository.existsByParent(firstCategory)) {
            throw new ApiException(ErrorCode.CATEGORY_HAS_SUBCATEGORIES);
        }

        // 1차 카테고리 삭제
        categoryRepository.delete(firstCategory);
    }

    @Transactional
    public SecondCategoryCreateResponse createSecondCategory(Long memberId, Long firstCategoryId, SecondCategoryCreateRequest request) {
        // 존재하지 않는 1차 카테고리일 경우 예외 발생
        Category firstCategory = categoryRepository.findById(firstCategoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST));

        if (categoryRepository.existsByNameAndParent(request.getName(),firstCategory)) {
            throw new ApiException(ErrorCode.DUPLICATE_CATEGORY_SECOND);
        }

        // 2차 카테고리 생성 (부모 없음)
        Category secondCategory = new Category(firstCategory, request.getName());
        categoryRepository.save(secondCategory);

        return new SecondCategoryCreateResponse(secondCategory.getId());
    }

    @Transactional
    public void updateSecondCategory(Long memberId, Long firstCategoryId, Long secondCategoryId, SecondCategoryUpdateRequest request) {
        // 존재하지 않는 2차 카테고리 예외 처리
        Category secondCategory = categoryRepository.findById(secondCategoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_SECOND));

        // 2차 카테고리의 부모가 요청된 1차 카테고리인지 검증
        if (secondCategory.getParent() == null || !secondCategory.getParent().getId().equals(firstCategoryId)) {
            throw new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST);
        }

        // 동일한 이름의 2차 카테고리 존재 여부 확인 (자기 자신은 제외)
        if (categoryRepository.existsByNameAndParent(request.getSecondCategory(), secondCategory.getParent()) &&
                !secondCategory.getName().equals(request.getSecondCategory())) {
            throw new ApiException(ErrorCode.DUPLICATE_CATEGORY_SECOND);
        }

        // 2차 카테고리 이름 변경
        secondCategory.updateName(request.getSecondCategory());
    }

    @Transactional
    public void deleteSecondCategory(Long memberId, Long firstCategoryId, Long secondCategoryId) {
        // 존재하지 않는 2차 카테고리 예외 처리
        Category secondCategory = categoryRepository.findById(secondCategoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_SECOND));

        // 2차 카테고리의 부모가 요청된 1차 카테고리인지 검증
        if (secondCategory.getParent() == null || !secondCategory.getParent().getId().equals(firstCategoryId)) {
            throw new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST);
        }

        // 2차 카테고리 삭제
        categoryRepository.delete(secondCategory);
    }
}
