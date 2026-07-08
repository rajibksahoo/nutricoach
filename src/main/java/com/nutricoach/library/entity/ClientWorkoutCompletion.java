package com.nutricoach.library.entity;

import com.nutricoach.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Records that a client marked one of their derived upcoming/today workouts as
 * completed. Unique per (client, workout, date); soft-deleted via {@code deletedAt}.
 */
@Entity
@Table(name = "client_workout_completions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientWorkoutCompletion extends BaseEntity {

    @Column(name = "coach_id", nullable = false)
    private UUID coachId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "workout_id", nullable = false)
    private UUID workoutId;

    @Column(name = "workout_date", nullable = false)
    private LocalDate workoutDate;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
