package com.nutricoach.library.entity;

import com.nutricoach.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise extends BaseEntity {

    @Column(name = "coach_id", nullable = false)
    private UUID coachId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "muscle_group", length = 50)
    private String muscleGroup;

    @Column(length = 80)
    private String equipment;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(columnDefinition = "text")
    private String notes;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category = Category.strength;

    @Column(name = "movement_pattern", length = 60)
    private String movementPattern;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags")
    private List<String> tags;

    @Builder.Default
    @Column(name = "is_custom", nullable = false)
    private boolean custom = true;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum Category { strength, bodyweight, timed, cardio, amrap }
}
