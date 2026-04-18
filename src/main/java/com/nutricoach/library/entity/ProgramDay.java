package com.nutricoach.library.entity;

import com.nutricoach.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "program_days")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramDay extends BaseEntity {

    @Column(name = "program_id", nullable = false)
    private UUID programId;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Column(name = "workout_id")
    private UUID workoutId;

    @Column(columnDefinition = "text")
    private String notes;
}
