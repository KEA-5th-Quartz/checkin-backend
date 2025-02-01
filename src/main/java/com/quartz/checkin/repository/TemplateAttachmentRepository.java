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
    @Query("delete from TemplateAttachment ta where ta.template = :template and ta.attachment.id in :attachmentIds")
    void deleteByTemplateAndAttachmentIds(@Param("template") Template template,
                                          @Param("attachmentIds") List<Long> attachmentIds);
}
