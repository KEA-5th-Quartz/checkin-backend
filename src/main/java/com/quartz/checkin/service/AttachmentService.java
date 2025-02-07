package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.dto.common.response.UploadAttachmentsResponse;
import com.quartz.checkin.dto.ticket.response.AttachmentResponse;
import com.quartz.checkin.entity.Attachment;
import com.quartz.checkin.entity.TicketAttachment;
import com.quartz.checkin.repository.AttachmentRepository;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import com.quartz.checkin.repository.TicketAttachmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final S3Service s3Service;
    private final EntityManager entityManager;
    private final AttachmentRepository attachmentRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;

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
        attachmentRepository.deleteAllByIdInBatch(attachmentIdsToRemove);

        entityManager.flush();
        entityManager.clear();
    }

    // 파일 다운로드 정보 조회
    @Transactional(readOnly = true)
    public AttachmentResponse getAttachmentInfo(Long ticketId, String attachmentUrl) {
        // 1. 티켓-첨부파일 연결 정보 조회
        TicketAttachment ticketAttachment = ticketAttachmentRepository
                .findByTicketIdAndAttachmentUrl(ticketId, attachmentUrl)
                .orElseThrow(() -> new ApiException(ErrorCode.ATTACHMENT_NOT_FOUND));

        // 2. 첨부파일 상세 정보 조회
        Attachment attachment = (Attachment) attachmentRepository.findByUrl(attachmentUrl)
                .orElseThrow(() -> new ApiException(ErrorCode.ATTACHMENT_NOT_FOUND));

        // 3. 파일 이름 추출
        String fileName = extractFileNameFromUrl(attachment.getUrl());

        return new AttachmentResponse( // DTO 이름 통일
                attachment.getId(),
                fileName,
                attachment.getUrl()
        );
    }

    private String extractFileNameFromUrl(String url) {
        try {
            return Paths.get(new URI(url).getPath()).getFileName().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid URL format: " + url, e);
        }
    }

}
