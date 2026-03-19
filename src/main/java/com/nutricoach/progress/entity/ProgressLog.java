package com.nutricoach.progress.entity;

import com.nutricoach.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "progress_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgressLog extends BaseEntity {

    @Column(name = "coach_id", nullable = false)
    private UUID coachId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "logged_date", nullable = false)
    private LocalDate loggedDate;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "body_fat_percent", precision = 4, scale = 1)
    private BigDecimal bodyFatPercent;

    @Column(name = "waist_cm", precision = 5, scale = 1)
    private BigDecimal waistCm;

    @Column(name = "chest_cm", precision = 5, scale = 1)
    private BigDecimal chestCm;

    @Column(name = "hip_cm", precision = 5, scale = 1)
    private BigDecimal hipCm;

    @Column(name = "adherence_percent")
    private Integer adherencePercent;

    @Column(columnDefinition = "text")
    private String notes;
}
