package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.entity.Category;
import com.quartz.checkin.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public Category getFirstCategoryOrThrow(String firstCategory) {
        return categoryRepository.findByNameAndParentIsNull(firstCategory)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_FIRST));
    }

    public Category getSecondCategoryOrThrow(String secondCategory, Category firstCategory) {
        return categoryRepository.findByNameAndParent(secondCategory, firstCategory)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND_SECOND));
    }

}
