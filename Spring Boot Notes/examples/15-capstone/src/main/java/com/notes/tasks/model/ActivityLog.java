package com.notes.tasks.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** An audit trail row. Written in its own transaction (REQUIRES_NEW), so it survives rollbacks. */
@Entity
@Getter
@NoArgsConstructor
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String message;

    @Column(nullable = false)
    private Instant createdAt;

    public ActivityLog(String message, Instant createdAt) {
        this.message = message;
        this.createdAt = createdAt;
    }
}
