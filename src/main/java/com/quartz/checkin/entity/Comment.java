package com.quartz.checkin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

@Entity
@Getter
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Member member;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private String attachment;

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public void writeContent(String content) {
        this.content = content;
    }

    public void addAttachment(String attachment) {
        this.attachment = attachment;
    }

    public void hardDeleteMember(Member member) {
        this.member = member;
    }
}