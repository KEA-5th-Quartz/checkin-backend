package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    // 1차 카테고리 조회 (parent_id가 NULL)
    Optional<Category> findByNameAndParentIsNull(String name);

    // 특정 1차 카테고리의 하위 카테고리 조회
    Optional<Category> findByNameAndParent(String name, Category parent);

    //  1차 카테고리에 속하는 가장 첫 번째 2차 카테고리 조회
    List<Category> findByParentOrderByIdAsc(Category parent);

}

