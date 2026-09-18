package com.nutricoach.client.entity;

import com.nutricoach.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A coach-authored note about a client.
 *
 * <p>Distinct from {@code progress_logs.notes}, which annotates one dated
 * measurement — these are free-standing observations on the client.
 */
@Entity
@Table(name = "client_notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientNote extends BaseEntity {

    @Column(name = "coach_id", nullable = false)
    private UUID coachId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "body", nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
