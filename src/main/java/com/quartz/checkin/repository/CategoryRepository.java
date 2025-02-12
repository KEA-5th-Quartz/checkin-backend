package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameAndParentIsNull(String name);
    Optional<Category> findByNameAndParent(String name, Category parent);
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

