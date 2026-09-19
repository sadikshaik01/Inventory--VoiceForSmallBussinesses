package com.voicestock.entity;

import java.time.Instant;
import jakarta.persistence.*;

@MappedSuperclass
public abstract class AuditedEntity extends BaseEntity {
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void updateTimestamp() { updatedAt = Instant.now(); }

    public Instant getUpdatedAt() { return updatedAt; }
}
