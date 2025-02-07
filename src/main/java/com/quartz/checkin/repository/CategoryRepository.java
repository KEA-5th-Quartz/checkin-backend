package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    // 1차 카테고리 조회 (parent_id가 NULL)
    Optional<Category> findByNameAndParentIsNull(String name);

    // 특정 1차 카테고리의 하위 카테고리 조회
    Optional<Category> findByNameAndParent(String name, Category parent);

    //  1차 카테고리에 속하는 가장 첫 번째 2차 카테고리 조회
    List<Category> findByParentOrderByIdAsc(Category parent);

    boolean existsByNameAndParentIsNullAndIdNot(String name, Long id);
    boolean existsByAliasAndIdNot(String alias, Long id);
    boolean existsByNameAndParentIsNull(String name);
    boolean existsByAlias(String alias);
    boolean existsByNameAndParentAndIdNot(String name, Category parent, Long id);
    boolean existsByParent(Category parent);
    boolean existsByNameAndParent(String name, Category parent);
    List<Category> findByParentId(Long parentId);
}

