package com.quartz.checkin.repository;

import com.quartz.checkin.dto.category.response.CategoryResponse;
import com.quartz.checkin.dto.category.response.SecondCategoryResponse;
import com.quartz.checkin.entity.Category;
import com.quartz.checkin.entity.QCategory;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryCustomImpl implements CategoryRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CategoryResponse> findAllCategoriesWithSecondCategories() {
        QCategory firstCategory = new QCategory("firstCategory");
        QCategory secondCategory = new QCategory("secondCategory");

        List<Tuple> result = queryFactory
                .select(firstCategory, secondCategory)
                .from(firstCategory)
                .leftJoin(secondCategory).on(secondCategory.parent.eq(firstCategory))
                .where(firstCategory.parent.isNull())
                .orderBy(firstCategory.id.asc(), secondCategory.id.asc())
                .fetch();

        Map<Long, CategoryResponse> categoryMap = new LinkedHashMap<>();

        for (Tuple row : result) {
            Category first = row.get(firstCategory);
            Category second = row.get(secondCategory);

            categoryMap.putIfAbsent(Objects.requireNonNull(first).getId(),
                    new CategoryResponse(
                            first.getId(),
                            first.getName(),
                            first.getAlias(),
                            first.getContentGuide(),
                            new ArrayList<>()
                    ));

            if (second != null) {
                categoryMap.get(first.getId()).getSecondCategories()
                        .add(new SecondCategoryResponse(
                                second.getId(),
                                second.getName(),
                                second.getAlias()
                        ));
            }
        }

        return new ArrayList<>(categoryMap.values());
    }
}

