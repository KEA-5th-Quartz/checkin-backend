package com.quartz.checkin.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(nullable = false)
    private String name;

    @Column
    private String alias;

    @Column(name = "content_guide", columnDefinition = "TEXT")
    private String contentGuide;

    public Category(Category parent, String name, String alias, String contentGuide) {
        this.parent = parent;
        this.name = name;
        this.alias = alias;
        this.contentGuide = contentGuide;
    }

    public void updateCategory(String name, String alias, String contentGuide) {
        this.name = name;
        this.alias = alias;
        this.contentGuide = contentGuide;
    }
}
