package com.quartz.checkin.service;

import com.quartz.checkin.common.PasswordGenerator;
import com.quartz.checkin.common.exception.ApiException;
import com.quartz.checkin.common.exception.ErrorCode;
import com.quartz.checkin.config.S3Config;
import com.quartz.checkin.dto.common.request.SimplePageRequest;
import com.quartz.checkin.dto.member.request.MemberRegistrationRequest;
import com.quartz.checkin.dto.member.request.PasswordChangeRequest;
import com.quartz.checkin.dto.member.request.PasswordResetEmailRequest;
import com.quartz.checkin.dto.member.request.PasswordResetRequest;
import com.quartz.checkin.dto.member.request.MemberInfoListRequest;
import com.quartz.checkin.dto.member.request.RoleUpdateRequest;
import com.quartz.checkin.dto.member.response.MemberInfoListResponse;
import com.quartz.checkin.dto.member.response.MemberInfoResponse;
import com.quartz.checkin.dto.member.response.MemberRoleCount;
import com.quartz.checkin.entity.Comment;
import com.quartz.checkin.entity.Member;
import com.quartz.checkin.entity.MemberAccessLog;
import com.quartz.checkin.entity.Role;
import com.quartz.checkin.entity.Template;
import com.quartz.checkin.entity.TemplateAttachment;
import com.quartz.checkin.entity.Ticket;
import com.quartz.checkin.event.MemberRegisteredEvent;
import com.quartz.checkin.event.PasswordResetMailEvent;
import com.quartz.checkin.event.RoleUpdateEvent;
import com.quartz.checkin.repository.CommentRepository;
import com.quartz.checkin.repository.LikeRepository;
import com.quartz.checkin.repository.MemberAccessLogRepository;
import com.quartz.checkin.repository.MemberRepository;
import com.quartz.checkin.repository.TemplateAttachmentRepository;
import com.quartz.checkin.repository.TemplateRepository;
import com.quartz.checkin.repository.TicketRepository;
import com.quartz.checkin.security.CustomUser;
import com.quartz.checkin.security.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final AttachmentService attachmentService;
    private final ApplicationEventPublisher eventPublisher;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;
    private final JwtService jwtService;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final MemberAccessLogRepository memberAccessLogRepository;
    private final TemplateRepository templateRepository;
    private final TemplateAttachmentRepository templateAttachmentRepository;
    private final TicketRepository ticketRepository;

    public Member getMemberByIdOrThrow(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 사용자입니다.");
                    return new ApiException(ErrorCode.MEMBER_NOT_FOUND);
                });
    }

    public Member getMemberByUsernameOrThrow(String username) {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("존재하지 않는 사용자입니다.");
                    return new ApiException(ErrorCode.MEMBER_NOT_FOUND);
                });
    }

    public MemberInfoResponse getMemberInfo(Long id) {
        Member member = getMemberByIdOrThrow(id);

        return MemberInfoResponse.from(member);
    }

    public MemberInfoListResponse getMemberInfoList(MemberInfoListRequest memberInfoListRequest) {

        Role role = Role.fromValue(memberInfoListRequest.getRole());
        int page = memberInfoListRequest.getPage();
        int size = memberInfoListRequest.getSize();
        String username = memberInfoListRequest.getUsername();

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("id").ascending());

        Page<Member> memberPage = null;
        if (username == null || username.isBlank()) {
            memberPage = memberRepository.findByRoleAndDeletedAtIsNull(role, pageable);
        } else {
            memberPage = memberRepository.findByRoleAndUsernameContainingAndDeletedAtIsNull(role, username, pageable);
        }

        return MemberInfoListResponse.from(memberPage);
    }

    public MemberInfoListResponse getSoftDeletedMemberInfoList(SimplePageRequest simplePageRequest) {

        int page = simplePageRequest.getPage();
        int size = simplePageRequest.getSize();

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("deletedAt").ascending());

        Page<Member> memberPage = memberRepository.findByDeletedAtIsNotNull(pageable);

        return MemberInfoListResponse.from(memberPage);
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
    public void changeMemberPassword(Long id, CustomUser customUser, PasswordChangeRequest passwordChangeRequest) {
        Member member = getMemberByIdOrThrow(id);

        checkMemberOwnsResource(member, customUser);

        String originalPassword = passwordChangeRequest.getOriginalPassword();
        String newPassword = passwordChangeRequest.getNewPassword();

        if (!passwordEncoder.matches(originalPassword, member.getPassword())) {
            log.error("기존 비밀번호와 일치하지 않습니다.");
            throw new ApiException(ErrorCode.INVALID_ORIGINAL_PASSWORD);
        }

        if (originalPassword.equals(newPassword)) {
            log.error("새 비밀번호가 기존 비밀번호와 일치합니다.");
            throw new ApiException(ErrorCode.INVALID_NEW_PASSWORD);
        }

        member.updatePassword(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public void resetMemberPassword(Long id, PasswordResetRequest passwordResetRequest) {
        Member member = getMemberByIdOrThrow(id);

        String passwordResetToken = passwordResetRequest.getPasswordResetToken();
        if (!jwtService.isValidToken(passwordResetToken)) {
            log.error("유효하지 않은 비밀번호 초기화 토큰입니다.");
            throw new ApiException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }

        if (!jwtService.getMemberIdFromPasswordResetToken(passwordResetToken).equals(id)) {
            log.error("다른 사용자의 리소스에 접근하려고 합니다.");
            throw new ApiException(ErrorCode.FORBIDDEN);
        }

        member.updatePassword(passwordEncoder.encode(passwordResetRequest.getNewPassword()));
    }

    public void sendPasswordResetMail(PasswordResetEmailRequest passwordResetEmailRequest) {
        Member member = memberRepository.findByUsername(passwordResetEmailRequest.getUsername())
                .orElseThrow(() -> {
                    log.error("존재하지 않는 사용자입니다.");
                    return new ApiException(ErrorCode.MEMBER_NOT_FOUND);
                });

        String passwordResetToken = jwtService.createPasswordResetToken(member.getId());

        eventPublisher.publishEvent(new PasswordResetMailEvent(member.getId(), member.getEmail(), passwordResetToken));
    }

    @Transactional
    public void updateMemberRefreshToken(Long id, String refreshToken) {
        Member member = getMemberByIdOrThrow(id);

        member.updateRefreshToken(refreshToken);
    }

    @Transactional
    public String updateMemberProfilePic(Long id, CustomUser customUser, MultipartFile file) {
        if (file.isEmpty() || !s3Service.isImageType(file.getContentType())) {
            log.error("파일이 누락되었거나, 이미지 타입이 아닙니다.");
            throw new ApiException(ErrorCode.INVALID_DATA);
        }

        long fileSize = file.getSize();
        if (fileSize > S3Config.MAX_IMAGE_SIZE) {
            log.error("파일이 최대 용량을 초과했습니다.");
            throw new ApiException(ErrorCode.TOO_LARGE_FILE);
        }

        Member member = getMemberByIdOrThrow(id);

        checkMemberOwnsResource(member, customUser);

        try {
            String profilePic = s3Service.uploadFile(file, S3Config.PROFILE_DIR);
            member.updateProfilePic(profilePic);

            return profilePic;
        } catch (Exception exception) {
            log.error("S3에 파일을 업로드할 수 없습니다. {}", exception.getMessage());
            throw new ApiException(ErrorCode.OBJECT_STORAGE_ERROR);
        }
    }

    @Transactional
    public void updateMemberRole(Long id, HttpServletRequest request, RoleUpdateRequest roleUpdateRequest) {
        Member member = getMemberByIdOrThrow(id);

        Role newRole = Role.fromValue(roleUpdateRequest.getRole());
        if (member.getRole().equals(newRole)) {
            log.error("기존 권한과 동일합니다.");
            throw new ApiException(ErrorCode.INVALID_NEW_ROLE);
        }

        member.updateRole(newRole);

        eventPublisher.publishEvent(new RoleUpdateEvent(member.getUsername()));
    }

    @Transactional
    public void softDeleteMember(Long id) {
        Member member = getMemberByIdOrThrow(id);

        if (member.getDeletedAt() != null) {
            log.error("이미 소프트 딜리트된 사용자입니다.");
            throw new ApiException(ErrorCode.MEMBER_ALREADY_SOFT_DELETED);
        }

        member.softDelete();
    }

    @Transactional
    public void hardDeleteMember(Long id) {
        Member member = getMemberByIdOrThrow(id);

        if (member.getDeletedAt() == null) {
            throw new ApiException(ErrorCode.MEMBER_NOT_SOFT_DELETED);
        }

        Member deletedUser = getMemberByIdOrThrow(-1L);

        List<Ticket> tickets = ticketRepository.findByUser(member);
        for (Ticket ticket : tickets) {
            ticket.hardDeleteUser(deletedUser);
        }

        tickets = ticketRepository.findByManager(member);
        for (Ticket ticket : tickets) {
            ticket.hardDeleteUser(deletedUser);
        }

        List<Comment> comments = commentRepository.findByMember(member);
        for (Comment comment : comments) {
            comment.hardDeleteMember(member);
        }

        likeRepository.deleteAllByMember(member);

        memberAccessLogRepository.deleteAllByMember(member);

        List<Template> templates = templateRepository.findAllByMember(member);
        List<Long> templateIds = templates.stream()
                .map(Template::getId)
                .toList();

        List<TemplateAttachment> templateAttachments =
                templateAttachmentRepository.findAllByTemplatesJoinFetch(templateIds);

        List<Long> templateAttachmentIds = templateAttachments.stream()
                .map(TemplateAttachment::getId)
                .toList();

        attachmentService.deleteAttachments(templateAttachmentIds);

        templateAttachmentRepository.deleteByTemplates(templates);

        templateRepository.deleteByTemplateIds(templateIds);

    }

    @Transactional
    public void restoreMember(Long id) {
        Member member = getMemberByIdOrThrow(id);

        if (member.getDeletedAt() == null) {
            log.error("소프트 딜리트된 사용자가 아닙니다.");
            throw new ApiException(ErrorCode.MEMBER_NOT_SOFT_DELETED);
        }

        member.restore();
    }

    private void checkMemberOwnsResource(Member member, CustomUser customUser) {
        if (!Objects.equals(member.getId(), customUser.getId())) {
            log.error("다른 사용자의 리소스에 접근하려고 합니다.");
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
    }

    public void checkEmailDuplicate(String email) {
        if (memberRepository.findByEmail(email).isPresent()) {
            log.error("사용중인 이메일입니다. {}", email);
            throw new ApiException(ErrorCode.DUPLICATE_EMAIL);
        }
    }

    public void checkUsernameDuplicate(String username) {
        if (memberRepository.findByUsername(username).isPresent()) {
            log.error("사용중인 사용자 이름입니다. {}", username);
            throw new ApiException(ErrorCode.DUPLICATE_USERNAME);
        }
    }

    public MemberRoleCount getMemberRoleCounts() {
        return memberRepository.findRoleCounts();
    }

}
