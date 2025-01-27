package com.quartz.checkin.service;

import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.config.S3Config;
import com.quartz.checkin.dto.request.MemberRegistrationRequest;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.event.MemberRegisteredEvent;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.common.PasswordGenerator;
import com.quartz.checkin.security.CustomUser;
import java.io.IOException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final ApplicationEventPublisher eventPublisher;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3UploadService s3UploadService;

    public Member getMemberByIdOrThrow(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 사용자입니다.");
                    return new ApiException(ErrorCode.MEMBER_NOT_FOUND);
                });
    }

    @Transactional
    public void register(MemberRegistrationRequest memberRegistrationRequest) {
        String username = memberRegistrationRequest.getUsername();
        String email = memberRegistrationRequest.getEmail();

        checkUsernameDuplicate(username);

        checkEmailDuplicate(email);

        String tempPassword = PasswordGenerator.generateRandomPassword();
        String password = passwordEncoder.encode(tempPassword);
        memberRepository.save(Member.from(memberRegistrationRequest, password));

        eventPublisher.publishEvent(new MemberRegisteredEvent(email, tempPassword));
    }

    @Transactional
    public void updateMemberRefreshToken(Long id, String refreshToken) {
        Member member = getMemberByIdOrThrow(id);

        member.updateRefreshToken(refreshToken);
    }

    @Transactional
    public String updateMemberProfilePic(Long id, CustomUser customUser, MultipartFile file) {
        if (file.isEmpty() || !s3UploadService.isImageType(file.getContentType())) {
            log.error("파일이 누락되었거나, 이미지 타입이 아닙니다.");
            throw new ApiException(ErrorCode.INVALID_DATA);
        }

        long fileSize = file.getSize();
        if (fileSize > S3Config.MAX_IMAGE_SIZE) {
            log.error("파일이 최대 용량을 초과했습니다.");
            throw new ApiException(ErrorCode.TOO_LARGE_FILE);
        }

        Member member = getMemberByIdOrThrow(id);

        if (!Objects.equals(member.getId(), customUser.getId())) {
            log.error("다른 사용자의 리소스에 접근하려고 합니다.");
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        try {
            String profilePic = s3UploadService.uploadFile(file, S3Config.PROFILE_DIR);
            member.updateProfilePic(profilePic);

            return profilePic;
        } catch (IOException exception) {
            log.error("S3에 파일을 업로드할 수 없습니다. {}", exception.getMessage());
            throw new ApiException(ErrorCode.OBJECT_STORAGE_ERROR);
        }
    }

    private void checkEmailDuplicate(String email) {
        if (memberRepository.findByEmail(email).isPresent()) {
            log.error("사용중인 이메일입니다. {}", email);
            throw new ApiException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    private void checkUsernameDuplicate(String username) {
        if (memberRepository.findByUsername(username).isPresent()) {
            log.error("사용중인 사용자 이름입니다. {}", username);
            throw new ApiException(ErrorCode.DUPLICATE_USERNAME);
        }
    }

}
