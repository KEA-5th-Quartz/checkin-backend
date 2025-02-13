package com.quartz.checkin.repository;

import com.quartz.checkin.dto.category.response.CategoryResponse;
import java.util.List;

public interface CategoryRepositoryCustom {
    List<CategoryResponse> findAllCategoriesWithSecondCategories();
}
