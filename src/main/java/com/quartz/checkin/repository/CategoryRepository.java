package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    // 1차 카테고리 조회 (parent_id가 NULL)
    Optional<Category> findByNameAndParentIsNull(String name);

    // 특정 1차 카테고리의 하위 카테고리 조회
    Optional<Category> findByNameAndParent(String name, Category parent);

    //  1차 카테고리에 속하는 가장 첫 번째 2차 카테고리 조회
    List<Category> findByParentOrderByIdAsc(Category parent);

    // 1차 & 2차 카테고리 동시 조회
    @Query(" SELECT c1, c2 FROM Category c1 LEFT JOIN Category c2 "
            + " ON c2.parent = c1 WHERE c1.parent IS NULL ORDER BY c1.id, c2.id ")
    List<Object[]> findAllCategoriesWithSecondCategories();

    boolean existsByNameAndParentIsNull(String name);
    boolean existsByNameAndParent(String name, Category parent);
    boolean existsByParent(Category parent);
}

