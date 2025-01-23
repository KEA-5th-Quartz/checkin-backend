package com.quartz.checkin.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
public class TicketLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id")
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Logtype logtype;

    @Column(nullable = false)
    private String content;
}