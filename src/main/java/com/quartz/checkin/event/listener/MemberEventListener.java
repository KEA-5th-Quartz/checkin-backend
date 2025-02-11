package com.quartz.checkin.event.listener;

import com.quartz.checkin.event.MemberHardDeletedEvent;
import com.quartz.checkin.event.MemberRegisteredEvent;
import com.quartz.checkin.event.MemberRestoredEvent;
import com.quartz.checkin.event.PasswordResetMailEvent;
import com.quartz.checkin.event.RoleUpdateEvent;
import com.quartz.checkin.event.SoftDeletedEvent;
import com.quartz.checkin.service.EmailSenderService;
import com.quartz.checkin.service.RoleUpdateCacheService;
import com.quartz.checkin.service.SoftDeletedMemberCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberEventListener {

    private final EmailSenderService emailSenderService;
    private final RoleUpdateCacheService roleUpdateCacheService;
    private final SoftDeletedMemberCacheService softDeletedMemberCacheService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberRegisteredEvent(MemberRegisteredEvent event) {
        log.info("사용자 생성 완료, 임시 비밀번호 전송");
        emailSenderService.sendSimpleMessage(event.getEmail(), "임시 비밀번호 안내", event.getTempPassword());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePasswordResetMailEvent(PasswordResetMailEvent event) {
        log.info("비밀번호 초기화 이메일 전송");
        String format = "https://kc.quartz-checkin.xyz/password-reset?memberId=%s&passwordResetToken=%s";
        emailSenderService.sendSimpleMessage(
                event.getEmail(),
                "비밀번호 초기화",
                String.format(format, event.getId(), event.getPasswordResetToken()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRoleUpdateEvent(RoleUpdateEvent event) {
        String username = event.getUsername();
        log.info("사용자 {}의 권한이 변경되었습니다. 사용자 권한 변경 캐시에 기록합니다.", username);
        roleUpdateCacheService.put(username);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRoleUpdateEvent(SoftDeletedEvent event) {
        String username = event.getUsername();
        log.info("사용자({})가 소프트 딜리트 되었습니다. 소프트 딜리트 캐시에 기록합니다.", username);
        softDeletedMemberCacheService.put(username);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRoleUpdateEvent(MemberRestoredEvent event) {
        String username = event.getUsername();
        log.info("사용자({})가 복구되었습니다. 소프트 딜리트 캐시에서 제거합니다.", username);
        softDeletedMemberCacheService.evict(username);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRoleUpdateEvent(MemberHardDeletedEvent event) {
        String username = event.getUsername();
        log.info("사용자({})가 영구삭제되었습니다. 소프트 딜리트 캐시에서 제거합니다.", username);
        softDeletedMemberCacheService.evict(username);
    }


}
