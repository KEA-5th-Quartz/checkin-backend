package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Template;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TemplateRepository extends JpaRepository<Template, Long> {

    @Query("SELECT t FROM Template t "
            + "JOIN FETCH t.firstCategory fc "
            + "JOIN FETCH t.secondCategory sc "
            + "WHERE t.id = :templateId")
    Optional<Template> findByIdJoinFetch(@Param("templateId") Long templateId);

}
