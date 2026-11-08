package com.kernelconsulting.plateforme.moderation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Trace d'une décision de modération. */
@Entity
@Table(name = "moderation_log")
@Getter
@NoArgsConstructor
public class ModerationLog {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private UUID moderatorId;

    @Column(nullable = false)
    private String targetType;

    @Column(nullable = false)
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModerationAction action;

    private String note;

    @Column(nullable = false)
    private Instant createdAt;

    public ModerationLog(UUID moderatorId, String targetType, UUID targetId, ModerationAction action, String note, Instant createdAt) {
        this.moderatorId = moderatorId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.action = action;
        this.note = note;
        this.createdAt = createdAt;
    }
}
