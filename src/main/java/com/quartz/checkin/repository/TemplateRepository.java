package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.Template;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TemplateRepository extends JpaRepository<Template, Long> {

    @Query("SELECT t FROM Template t "
            + "JOIN FETCH t.firstCategory fc "
            + "JOIN FETCH t.secondCategory sc "
            + "WHERE t.id = :templateId")
    Optional<Template> findByIdJoinFetch(@Param("templateId") Long templateId);

    @Query(value = "SELECT t FROM Template t "
            + "JOIN FETCH t.firstCategory fc "
            + "JOIN FETCH t.secondCategory sc "
            + "WHERE t.member = :member",
            countQuery = "SELECT COUNT(t) FROM Template t "
                    + "WHERE t.member = :member")
    Page<Template> findAllByMemberJoinFetch(@Param("member")Member member, Pageable pageable);
}
