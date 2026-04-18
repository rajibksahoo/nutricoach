package com.nutricoach.library.entity;

import com.nutricoach.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "programs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Program extends BaseEntity {

    @Column(name = "coach_id", nullable = false)
    private UUID coachId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
