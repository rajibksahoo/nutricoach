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

    @Column(name = "weeks")
    private Integer weeks;

    @Column(name = "modality", length = 60)
    private String modality;

    @Column(name = "experience_level", length = 40)
    private String experienceLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags")
    private List<String> tags;

    @Column(name = "cover_s3_key", length = 500)
    private String coverS3Key;

    @Column(name = "cover_gradient", length = 120)
    private String coverGradient;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
