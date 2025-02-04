package com.quartz.checkin.service;

import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Client s3Client;
    @Value("${cloud.aws.bucket}")
    private String bucket;

    public String uploadFile(MultipartFile file, String dirName) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String fileName = UUID.randomUUID() + extension;
        String key = dirName + "/" + fileName;
        String contentType = getContentType(file.getContentType());

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        return String.format(
                "https://%s.s3.%s.amazonaws.com/%s",
                bucket,
                s3Client.serviceClientConfiguration().region().id(),
                key
        );
    }

    public boolean isImageType(String contentType) {
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith("image/");
    }

    private String getExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int idx = fileName.lastIndexOf(".");
        if (idx == -1) {
            return "";
        }
        return fileName.substring(idx);
    }

    private String getContentType(String contentType) {
        if (contentType == null) {
            return "application/octet-stream";
        }
        return contentType;
    }
}
