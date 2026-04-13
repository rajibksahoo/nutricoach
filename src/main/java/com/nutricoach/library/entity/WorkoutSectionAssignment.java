package com.nutricoach.library.entity;

import com.nutricoach.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "workout_section_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutSectionAssignment extends BaseEntity {

    @Column(name = "workout_id", nullable = false)
    private UUID workoutId;

    @Column(name = "section_id", nullable = false)
    private UUID sectionId;

    @Builder.Default
    @Column(nullable = false)
    private int position = 0;
}
