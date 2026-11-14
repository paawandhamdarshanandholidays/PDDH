package com.pddh.events_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "content_items")
public class ContentItem {
    @Id
    @Column(nullable = false, length = 36)
    private String id;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "images", columnDefinition = "bytea")
    private byte[] images;
}
