package com.quartz.checkin.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    public static final String COMMENT_DIR = "comment";
    public static final String PROFILE_DIR = "profile";
    public static final String TEMPLATE_DIR = "template";
    public static final String TICKET_DIR = "ticket";

    public static final Long MAX_IMAGE_SIZE = 5242880L; // 5mb

    @Value("${cloud.aws.region.static}")
    private String region;
    @Value("${cloud.aws.credentials.accessKey}")
    private String accessKey;
    @Value("${cloud.aws.credentials.secretKey}")
    private String secretKey;
    @Value("${cloud.aws.endpoint}")
    private String endpoint; // 카카오클라우드 엔드포인트

    @Bean
    public S3Client s3Client() {

        return S3Client.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint)) // endpoint를 카카오클라우드로 덮어쓰기
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .forcePathStyle(true)   // 카카오클라우드는 path style로 요청해야함
                .build();
    }
}
