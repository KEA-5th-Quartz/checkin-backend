package com.quartz.checkin.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Ticket extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private Member user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Member manager;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "first_category_id")
    private Category firstCategory;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "second_category_id")
    private Category secondCategory;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDateTime closedAt;

    private LocalDateTime deletedAt;

    @Builder
    public Ticket(Member user, Category firstCategory, Category secondCategory, String title, String content, Status status, LocalDate dueDate) {
        this.user = user;
        this.firstCategory = firstCategory;
        this.secondCategory = secondCategory;
        this.title = title;
        this.content = content;
        this.status = status;
        this.dueDate = dueDate;
    }

    // 담당자 할당 메서드
    public void assignManager(Member manager) {
        this.manager = manager;
        this.status = Status.IN_PROGRESS;  // 담당자 배정 시 상태 변경
    }

    // 담당자 변경 메서드
    public void reassignManager(Member newManager) {
        this.manager = newManager;
        this.status = Status.IN_PROGRESS; // 담당자 변경 시 진행 중 상태 유지
    }

    // 티켓 완료
    public void closeTicket() {
        this.status = Status.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    // 엔티티 내에서 카테고리 변경을 수행하도록 추가
    public void updateCategory(Category newFirstCategory, Category newSecondCategory) {
        this.firstCategory = newFirstCategory;
        this.secondCategory = newSecondCategory;
    }

    // 중요도 변경
    public void updatePriority(Priority newPriority) {
        this.priority = newPriority;
    }
}
