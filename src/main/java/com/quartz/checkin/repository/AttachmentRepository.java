package com.quartz.checkin.repository;

import com.quartz.checkin.entity.Attachment;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    void deleteAllByIdIn(List<Long> attachmentIds);

    Optional<Object> findByUrl(String attachmentUrl);

}
