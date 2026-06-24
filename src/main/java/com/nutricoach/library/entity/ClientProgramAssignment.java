package com.nutricoach.library.entity;

import com.nutricoach.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "client_program_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientProgramAssignment extends BaseEntity {

    @Column(name = "coach_id", nullable = false)
    private UUID coachId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
