package com.notes.tasks.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** A value object with no table or id of its own: its columns live in the owning entity's table. */
@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuditInfo {

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, length = 30)
    private String createdBy;
}
