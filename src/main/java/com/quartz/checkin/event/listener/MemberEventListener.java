package com.quartz.checkin.event.listener;

import com.quartz.checkin.event.MemberRegisteredEvent;
import com.quartz.checkin.service.EmailSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberEventListener {

    private final EmailSenderService emailSenderService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberRegisteredEvent(MemberRegisteredEvent event) {
        log.info("사용자 생성 완료, 임시 비밀번호 전송");
        emailSenderService.sendSimpleMessage(event.getEmail(), "임시 비밀번호 안내", event.getTempPassword());
    }
}
