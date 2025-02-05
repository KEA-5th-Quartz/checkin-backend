package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.common.response.UploadAttachmentsResponse;
import com.quartz.checkin.entity.Attachment;
import com.quartz.checkin.repository.AttachmentRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final S3Service s3Service;

    @Transactional
    public List<UploadAttachmentsResponse> uploadAttachments(List<MultipartFile> multipartFiles, String dirName) {

        for (MultipartFile multipartFile : multipartFiles) {
            if (multipartFile.isEmpty()) {
                log.error("첨부된 파일을 찾을 수 없습니다. {}", multipartFile.getOriginalFilename());
                throw new ApiException(ErrorCode.INVALID_DATA);
            }
        }

        List<Attachment> attachments = new ArrayList<>();
        for (MultipartFile multipartFile : multipartFiles) {
            try {
                String url = s3Service.uploadFile(multipartFile, dirName);
                attachments.add(new Attachment(url));
            } catch (Exception exception) {
                log.error("S3에 파일을 업로드할 수 없습니다. {}", exception.getMessage());
                throw new ApiException(ErrorCode.OBJECT_STORAGE_ERROR);
            }
        }

        return attachmentRepository.saveAll(attachments)
                .stream()
                .map(a -> new UploadAttachmentsResponse(a.getId(), a.getUrl()))
                .toList();

    }

    @Transactional
    public void deleteAttachments(List<Long> attachmentIdsToRemove) {
        // 제거해야 할 첨부파일 s3에서 삭제
        List<Attachment> attachmentsToDelete = attachmentRepository.findAllById(attachmentIdsToRemove);
        for (Attachment attachment : attachmentsToDelete) {
            try {
                s3Service.deleteFile(attachment.getUrl());
            } catch (Exception e) {
                log.error("파일 삭제에 실패했습니다.");
            }
        }
        attachmentRepository.deleteAllByIdIn(attachmentIdsToRemove);
    }
}
