package com.quartz.checkin.service;

import com.quartz.checkin.dto.response.CategoryResponse;
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
}
