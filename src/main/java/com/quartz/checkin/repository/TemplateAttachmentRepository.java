package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Template;
import com.quartz.checkin.entity.TemplateAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TemplateAttachmentRepository extends JpaRepository<TemplateAttachment, Long> {
    List<TemplateAttachment> findByTemplate(Template template);

    @Modifying
    @Query("DELETE FROM TemplateAttachment ta WHERE ta.template = :template AND ta.attachment.id IN :attachmentIds")
    void deleteByTemplateAndAttachmentIds(@Param("template") Template template,
                                          @Param("attachmentIds") List<Long> attachmentIds);

    @Modifying
    @Query("DELETE FROM TemplateAttachment ta WHERE ta.template IN :templates")
    void deleteByTemplates(@Param("templates") List<Template> templates);

    @Query("SELECT ta FROM TemplateAttachment ta " +
            "JOIN FETCH ta.attachment a " +
            "WHERE ta.template = :template")
    List<TemplateAttachment> findAllByTemplateJoinFetch(@Param("template") Template template);
}
