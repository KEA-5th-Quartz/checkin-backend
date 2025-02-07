package com.quartz.checkin.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

    @Column(name = "custom_id", nullable = false)
    private String customId;

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
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDateTime closedAt;

    private LocalDateTime deletedAt;

    @Column(name = "agit_id", nullable = true)
    private Long agitId;

    @Getter
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketAttachment> attachments;

    @Builder
    public Ticket(String customId, Member user, Category firstCategory, Category secondCategory, String title, String content,
                  Priority priority,Status status, LocalDate dueDate, Long agitId) {
        /*this.customId = customId;*/
        this.user = user;
        this.firstCategory = firstCategory;
        this.secondCategory = secondCategory;
        this.title = title;
        this.content = content;
        this.priority = priority;
        this.status = status;
        this.dueDate = dueDate;
        this.agitId = agitId;
    }

    public void linkToAgit(Long agitId) {
        if (this.agitId != null) {
            throw new IllegalStateException("아지트 ID는 이미 설정되어 있습니다.");
        }
        this.agitId = agitId;
    }

    public void unlinkFromAgit() {
        this.agitId = null;
    }

    public void updateAgitId(Long agitId) {
        this.agitId = agitId;
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

    // 티켓 완료 처리 메서드
    public void closeTicket() {
        this.status = Status.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    // 카테고리 변경 메서드
    public void updateCategory(Category newFirstCategory, Category newSecondCategory) {
        this.firstCategory = newFirstCategory;
        this.secondCategory = newSecondCategory;
    }

    // 중요도 변경 메서드
    public void updatePriority(Priority newPriority) {
        this.priority = newPriority;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void updateCustomId(String customId) {
        this.customId = customId;
    }

    public void updateCategories(Category firstCategory, Category secondCategory) {
        this.firstCategory = firstCategory;
        this.secondCategory = secondCategory;
    }

    public void updateDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void restoreTicket() {
        this.deletedAt = null;
    }
}
