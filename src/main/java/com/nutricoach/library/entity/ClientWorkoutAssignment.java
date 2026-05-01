package com.nutricoach.library.entity;

import com.nutricoach.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "client_workout_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientWorkoutAssignment extends BaseEntity {

    @Column(name = "coach_id", nullable = false)
    private UUID coachId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "workout_id", nullable = false)
    private UUID workoutId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
