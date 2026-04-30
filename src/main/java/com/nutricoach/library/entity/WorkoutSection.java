package com.nutricoach.library.entity;

import com.nutricoach.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workout_sections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutSection extends BaseEntity {

    @Column(name = "coach_id", nullable = false)
    private UUID coachId;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_type", nullable = false, length = 20)
    private Type sectionType;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum Type { WARM_UP, MAIN, ACCESSORY, COOL_DOWN, FINISHER }
}
