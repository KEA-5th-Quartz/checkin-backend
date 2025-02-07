package com.quartz.checkin.event.listener;

import com.quartz.checkin.event.MemberRegisteredEvent;
import com.quartz.checkin.event.PasswordResetMailEvent;
import com.quartz.checkin.event.RoleUpdateEvent;
import com.quartz.checkin.service.EmailSenderService;
import com.quartz.checkin.service.RoleUpdateCacheService;
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
        String format = "http://localhost:8080/password-reset?memberId=%s&passwordResetToken=%s";
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

}
