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
import com.quartz.checkin.entity.Category;
import com.quartz.checkin.repository.CategoryRepository;
import com.quartz.checkin.repository.TicketRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final TicketRepository ticketRepository;

    @Override
    public List<CategoryResponse> getAllCategories(Long memberId) {
        return categoryRepository.findAllCategoriesWithSecondCategories();
    }

    @Override
    public FirstCategoryCreateResponse createFirstCategory(Long memberId, FirstCategoryCreateRequest request) {
        checkDuplicateFirstCategory(request.getName(), null);
        checkDuplicateAlias(request.getAlias(), null);

        Category firstCategory = new Category(null, request.getName(), request.getAlias(), request.getContentGuide());
        categoryRepository.save(firstCategory);

        return new FirstCategoryCreateResponse(firstCategory.getId());
    }


    @Override
    public void updateFirstCategory(Long memberId, Long firstCategoryId, FirstCategoryUpdateRequest request) {
        Category firstCategory = getValidFirstCategory(firstCategoryId);

        // 본인의 기존 카테고리 이름과 다를 경우에만 중복 검사 수행
        if (!firstCategory.getName().equals(request.getName())) {
            checkDuplicateFirstCategory(request.getName(), firstCategoryId);
        }

        if (request.getAlias() != null && !request.getAlias().equals(firstCategory.getAlias())) {
            checkDuplicateAlias(request.getAlias(), firstCategoryId);
        }

        firstCategory.updateCategory(request.getName(), request.getAlias(), request.getContentGuide());
    }

    @Override
    public void deleteFirstCategory(Long memberId, Long firstCategoryId) {
        Category firstCategory = getValidFirstCategory(firstCategoryId);

        if (categoryRepository.existsByParent(firstCategory)) {
            throw new ApiException(ErrorCode.CATEGORY_HAS_SUBCATEGORIES);
        }
        boolean isCategoryInUse = ticketRepository.existsByFirstCategory(firstCategory);

        if (isCategoryInUse) {
            throw new ApiException(ErrorCode.FIRST_CATEGORY_IN_USE);
        }

        categoryRepository.delete(firstCategory);
    }

    @Override
    public SecondCategoryCreateResponse createSecondCategory(Long memberId, Long firstCategoryId, SecondCategoryCreateRequest request) {
        Category firstCategory = getValidFirstCategory(firstCategoryId);
        checkDuplicateSecondCategory(request.getName(), firstCategory, null);
        validateDuplicateSecondCategoryAlias(firstCategory, request.getAlias(), null);

        Category secondCategory = new Category(firstCategory, request.getName(), request.getAlias(), null);
        categoryRepository.save(secondCategory);

        return new SecondCategoryCreateResponse(secondCategory.getId());
    }

    @Override
    public void updateSecondCategory(Long memberId, Long firstCategoryId, Long secondCategoryId, SecondCategoryUpdateRequest request) {
        Category secondCategory = getValidSecondCategory(firstCategoryId, secondCategoryId);

        if (!secondCategory.getName().equals(request.getSecondCategory())) {
            checkDuplicateSecondCategory(request.getSecondCategory(), secondCategory.getParent(), secondCategoryId);
        }

        if (!secondCategory.getAlias().equals(request.getAlias())) {
            validateDuplicateSecondCategoryAlias(secondCategory.getParent(), request.getAlias(), secondCategoryId);
        }

        secondCategory.updateCategory(request.getSecondCategory(), request.getAlias(), null);
    }

    @Override
    public void deleteSecondCategory(Long memberId, Long firstCategoryId, Long secondCategoryId) {
        Category secondCategory = getValidSecondCategory(firstCategoryId, secondCategoryId);
        boolean isCategoryInUse = ticketRepository.existsBySecondCategory(secondCategory);

        if (isCategoryInUse) {
            throw new ApiException(ErrorCode.SECOND_CATEGORY_IN_USE);
        }

        categoryRepository.delete(secondCategory);
    }

    private Category getValidFirstCategory(Long firstCategoryId) {
        Category firstCategory = categoryRepository.findById(firstCategoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST));

        if (firstCategory.getParent() != null) {
            throw new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST);
        }

        return firstCategory;
    }

    private Category getValidSecondCategory(Long firstCategoryId, Long secondCategoryId) {
        Category secondCategory = categoryRepository.findById(secondCategoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_SECOND));

        if (secondCategory.getParent() == null || !secondCategory.getParent().getId().equals(firstCategoryId)) {
            throw new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST);
        }

        return secondCategory;
    }

    private void checkDuplicateFirstCategory(String name, Long excludeId) {
        boolean exists = Optional.ofNullable(excludeId)
                .map(id -> categoryRepository.existsByNameAndParentIsNullAndIdNot(name, id))
                .orElse(categoryRepository.existsByNameAndParentIsNull(name));

        if (exists) {
            throw new ApiException(ErrorCode.DUPLICATE_CATEGORY_FIRST);
        }
    }

    private void checkDuplicateAlias(String alias, Long excludeId) {
        boolean exists = Optional.ofNullable(excludeId)
                .map(id -> categoryRepository.existsByAliasAndIdNot(alias, id))
                .orElse(categoryRepository.existsByAlias(alias));

        if (exists) {
            throw new ApiException(ErrorCode.DUPLICATE_ALIAS);
        }
    }

    private void validateDuplicateSecondCategoryAlias(Category firstCategory, String alias, Long excludeId) {
        List<Category> secondCategories = categoryRepository.findByParentId(firstCategory.getId());

        Set<String> existingAliases = new HashSet<>();
        for (Category secondCategory : secondCategories) {
            if (!secondCategory.getId().equals(excludeId)) {
                existingAliases.add(secondCategory.getAlias());
            }
        }

        if (existingAliases.contains(alias)) {
            throw new ApiException(ErrorCode.DUPLICATE_ALIAS);
        }
    }

    private void checkDuplicateSecondCategory(String name, Category parent, Long excludeId) {
        boolean exists = (excludeId == null)
                ? categoryRepository.existsByNameAndParent(name, parent)
                : categoryRepository.existsByNameAndParentAndIdNot(name, parent, excludeId);

        if (exists) {
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
}
