package com.quartz.checkin.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationEvent {
    private String relatedId;      // 관련 엔터티 ID
    private String type;         // 알림 유형 (예: COMMENT, STATUS_CHANGE, ASSIGNEE_CHANGED 등)
    private String relatedTable; // 관련 테이블 이름
    private Long memberId;       // 이벤트를 발생시킨 사람 (댓글 작성자)
    private Long userId;         // 티켓을 등록한 사용자
    private Long managerId;      // 기존 담당자 (댓글 이벤트에서 사용)
    private Long newManagerId;   // 변경된 담당자 (ASSIGNEE_CHANGED에서 사용)
    private Long agitId;
    private String logMessage;
}